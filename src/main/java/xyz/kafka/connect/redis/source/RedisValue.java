package xyz.kafka.connect.redis.source;

import cn.hutool.core.lang.Pair;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.redisson.ScanResult;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.api.SortOrder;
import org.redisson.api.StreamMessageId;
import org.redisson.client.protocol.ScoredEntry;
import org.redisson.client.protocol.decoder.ListScanResult;
import org.redisson.client.protocol.decoder.MapScanResult;
import xyz.kafka.connector.redis.RedisConstants;
import xyz.kafka.utils.StringUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static xyz.kafka.connect.redis.source.RedisSourceConnectorConfig.SCAN_POINTER_LATEST;

/**
 * RedisValue
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2023-08-28
 */
@AllArgsConstructor
@Getter
@SuppressWarnings("unchecked")
public enum RedisValue {
    /**
     *
     */
    STRING(
            "string",
            Schema.OPTIONAL_STRING_SCHEMA
    ) {
        @Override
        <T> Object toConnectData(T result) {
            return StringUtil.toString(result);
        }

        @Override
        <T> ScanResult<T> scan(Pair<String, String> pair, long start, long end, int count, RedissonClient redisson) {
            Iterable<String> keyItr = redisson.getKeys().getKeysByPattern(pair.getKey());
            long endIdx = redisson.getStream(pair.getKey()).size() - 1;
            String next = String.valueOf(end);
            if (end > endIdx) {
                next = SCAN_POINTER_LATEST;
            }
            String[] keys = StreamSupport.stream(keyItr.spliterator(), false)
                    .sorted()
                    .skip(start)
                    .limit(count)
                    .toArray(String[]::new);
            List<T> values = (List<T>) redisson.getBuckets().get(keys)
                    .values()
                    .stream()
                    .toList();
            return new ListScanResult<>(next, values);
        }
    },
    HASH(
            "hash",
            SchemaBuilder.struct()
                    .field(RedisConstants.FIELD, Schema.STRING_SCHEMA)
                    .field(RedisConstants.VALUE, Schema.STRING_SCHEMA)
                    .optional()
                    .build()
    ) {
        @Override
        <T> Object toConnectData(T result) {
            Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) result;
            return new Struct(HASH.schema)
                    .put(RedisConstants.FIELD, StringUtil.toString(entry.getKey()))
                    .put(RedisConstants.VALUE, StringUtil.toString(entry.getValue()));
        }

        @Override
        <T> ScanResult<T> scan(Pair<String, String> pair, long start, long end, int count, RedissonClient redisson) {
            RMap<Object, Object> map = redisson.getMap(pair.getKey());
            Set<Object> keys = map.keySet(pair.getValue())
                    .stream()
                    .sorted()
                    .skip(start)
                    .limit(count)
                    .collect(Collectors.toSet());
            Map<Object, Object> all = map.getAll(keys);
            long endIdx = redisson.getStream(pair.getKey()).size() - 1;
            String next = String.valueOf(end);
            if (end > endIdx) {
                next = SCAN_POINTER_LATEST;
            }
            return (ScanResult<T>) new MapScanResult<>(next, all);
        }
    },
    LIST(
            "list",
            Schema.OPTIONAL_STRING_SCHEMA
    ) {
        @Override
        <T> Object toConnectData(T result) {
            return StringUtil.toString(result);
        }

        @Override
        <T> ScanResult<T> scan(Pair<String, String> pair, long start, long end, int count, RedissonClient redisson) {
            String stop = String.valueOf(end);
            long endIdx = redisson.getStream(pair.getKey()).size() - 1;
            String next = stop;
            if (end > endIdx) {
                end = endIdx;
                next = SCAN_POINTER_LATEST;
            }
            List<T> values = (List<T>) redisson.getList(pair.getKey()).range((int) start, (int) end);
            return new ListScanResult<>(next, values);
        }
    },
    SET(
            "set",
            Schema.OPTIONAL_STRING_SCHEMA
    ) {
        @Override
        <T> Object toConnectData(T result) {
            return StringUtil.toString(result);
        }

        @Override
        <T> ScanResult<T> scan(Pair<String, String> pair, long start, long end, int count, RedissonClient redisson) {
            String stop = String.valueOf(end);
            long endIdx = redisson.getStream(pair.getKey()).size() - 1;
            String next = stop;
            if (end > endIdx) {
                next = SCAN_POINTER_LATEST;
            }
            List<T> list = (List<T>) redisson.getSet(pair.getKey())
                    .readSort(SortOrder.ASC, (int) start, count)
                    .stream()
                    .toList();
            return new ListScanResult<>(next, list);
        }

    },
    ZSET(
            "zset",
            SchemaBuilder.struct()
                    .field(RedisConstants.SCORE, Schema.FLOAT64_SCHEMA)
                    .field(RedisConstants.MEMBER, Schema.STRING_SCHEMA)
                    .optional()
                    .build()
    ) {
        @Override
        <T> Object toConnectData(T result) {
            ScoredEntry<Object> entry = (ScoredEntry<Object>) result;
            return new Struct(ZSET.schema)
                    .put(RedisConstants.SCORE, entry.getScore())
                    .put(RedisConstants.MEMBER, StringUtil.toString(entry.getValue()));
        }

        @Override
        <T> ScanResult<T> scan(Pair<String, String> pair, long start, long end, int count, RedissonClient redisson) {
            RScoredSortedSet<Object> sortedSet = redisson.getScoredSortedSet(pair.getKey());
            long len = sortedSet.size() - 1L;
            String next = String.valueOf(end);
            if (end > len) {
                next = SCAN_POINTER_LATEST;
                end = len - 1;
            }
            List<ScoredEntry<Object>> entries = sortedSet.entryRange((int) start, (int) end)
                    .stream()
                    .toList();
            return new ListScanResult<>(next, (List<T>) entries);
        }
    },
    STREAM(
            "stream",
            SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.STRING_SCHEMA).build()
    ) {
        @Override
        <T> Object toConnectData(T result) {
            Map<Object, Object> map = (Map<Object, Object>) result;
            return map.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            e -> StringUtil.toString(e.getKey()),
                            e -> StringUtil.toString(e.getValue())
                    ));
        }

        @Override
        <T> ScanResult<T> scan(Pair<String, String> pair, long start, long end, int size, RedissonClient redisson) {
            long len = redisson.getStream(pair.getKey()).size() - 1;
            String next = String.valueOf(end);
            if (end > len) {
                end = len - 1;
                next = SCAN_POINTER_LATEST;
            }
            List<Map<Object, Object>> values =
                    redisson.getStream(pair.getKey())
                            .range(size, new StreamMessageId(start), new StreamMessageId(end))
                            .values()
                            .stream()
                            .toList();
            return new ListScanResult<>(next, (List<T>) values);
        }
    };

    private final String name;
    private final Schema schema;

    abstract <T> Object toConnectData(T result);

    abstract <T> ScanResult<T> scan(Pair<String, String> pair, long start, long end, int count, RedissonClient redisson);
}
