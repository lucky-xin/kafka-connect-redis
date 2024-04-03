package xyz.kafka.connect.redis.source;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson2.JSON;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;
import org.redisson.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.kafka.connect.redis.Versions;
import xyz.kafka.connector.config.RedissonConfig;
import xyz.kafka.connector.source.RateLimitSourceTask;
import xyz.kafka.utils.StringUtil;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static xyz.kafka.connect.redis.AbstractRedisConnectorConfig.CONNECTION_PASSWORD;
import static xyz.kafka.connect.redis.AbstractRedisConnectorConfig.CONNECTION_URL;
import static xyz.kafka.connect.redis.AbstractRedisConnectorConfig.CONNECTION_USER;
import static xyz.kafka.connect.redis.source.RedisSourceConnectorConfig.SCAN_POINTER_START;

/**
 * Kafka Connect Task for Kafka Connect Redis Sink.
 *
 * @author chaoxin.lu
 */
public class RedisSourceTask extends RateLimitSourceTask {
    private static final Logger log = LoggerFactory.getLogger(RedisSourceTask.class);

    private RedisSourceConnectorConfig config;
    private String pollOffsetKey;

    @Override
    public String version() {
        return Versions.VERSION;
    }

    @Override
    public void start(final Map<String, String> configs) {
        try {
            Map<String, String> m = append(configs);
            this.config = new RedisSourceConnectorConfig(m);
            this.pollOffsetKey = "redis_source_task:poll_offset_key:" + StringUtil.md5(JSON.toJSONString(configs));
            super.start(m);
        } catch (ConfigException ce) {
            log.error("task configuration error", ce);
            throw new ConnectException("task configuration error");
        }
    }

    private static Map<String, String> append(Map<String, String> originals) {
        originals.putIfAbsent(RedissonConfig.REDIS_NODES, originals.get(CONNECTION_URL));
        originals.putIfAbsent(RedissonConfig.REDIS_USER, originals.get(CONNECTION_USER));
        originals.putIfAbsent(RedissonConfig.REDIS_PWD, originals.get(CONNECTION_PASSWORD));
        return originals;
    }

    @Override
    public List<SourceRecord> doPoll() {
        Map<String, String> map = this.redisson.getMap(this.pollOffsetKey);
        List<SourceRecord> records = new ArrayList<>(batchSize);
        for (RedisValue rv : config.redisValues) {
            pollRecords(rv, map, records);
            if (records.size() >= batchSize) {
                break;
            }
        }
        if (map.values().stream().allMatch(SCAN_POINTER_START::equals)) {
            map.replaceAll((k, v) -> RedisSourceConnectorConfig.SCAN_POINTER_LATEST.equals(v) ? SCAN_POINTER_START : v);
        }
        return records;
    }

    private <T> void pollRecords(RedisValue rv,
                                 Map<String, String> map,
                                 List<SourceRecord> records) {
        Long cursor = MapUtil.getLong(map, rv.getName(), 0L);
        if (!cursor.equals(-1L)) {
            long end = cursor + batchSize;
            List<Pair<String, String>> pairs = config.keyAndPatterns;
            for (Pair<String, String> pair : pairs) {
                ScanResult<T> res = rv.scan(pair, cursor, end, batchSize, redisson);
                cursor = Long.parseLong(res.getPos());
                if (cursor == 0) {
                    map.put(rv.getName(), RedisSourceConnectorConfig.SCAN_POINTER_LATEST);
                    break;
                }
                AtomicLong idx = new AtomicLong(cursor);
                for (T t : res.getValues()) {
                    Object connectData = rv.toConnectData(t);
                    Map<String, Object> offset = Map.of(
                            "name", rv.getName(),
                            "key.pattern", pair.getValue(),
                            "cursor", idx.getAndIncrement()
                    );
                    Struct keyStruct = new Struct(RedisSourceConnectorConfig.KEY_SCHEMA);
                    offset.forEach(keyStruct::put);
                    Struct value = new Struct(RedisSourceConnectorConfig.VALUE_SCHEMA)
                            .put(rv.getName(), connectData);
                    SourceRecord sr = new SourceRecord(
                            Collections.emptyMap(),
                            offset,
                            config.topic,
                            null,
                            keyStruct.schema(),
                            keyStruct,
                            value.schema(),
                            value,
                            Clock.systemDefaultZone().instant().toEpochMilli()
                    );
                    records.add(sr);
                }
            }
            map.put(rv.getName(), String.valueOf(cursor));
        }
    }

    @Override
    public void stop() {
    }
}

