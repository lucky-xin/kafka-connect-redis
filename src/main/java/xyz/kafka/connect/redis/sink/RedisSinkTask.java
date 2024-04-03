package xyz.kafka.connect.redis.sink;

import cn.hutool.core.lang.Pair;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.ErrantRecordReporter;
import org.apache.kafka.connect.sink.SinkRecord;
import org.redisson.api.BatchOptions;
import org.redisson.api.RBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.kafka.connect.redis.Versions;
import xyz.kafka.connector.config.RedissonConfig;
import xyz.kafka.connector.enums.RedisClientType;
import xyz.kafka.connector.redis.RedisCommand;
import xyz.kafka.connector.redis.RedisConstants;
import xyz.kafka.connector.sink.RateLimitSinkTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static xyz.kafka.connect.redis.AbstractRedisConnectorConfig.CONNECTION_PASSWORD;
import static xyz.kafka.connect.redis.AbstractRedisConnectorConfig.CONNECTION_URL;
import static xyz.kafka.connect.redis.AbstractRedisConnectorConfig.CONNECTION_USER;

/**
 * Kafka Connect Task for Kafka Connect Redis Sink.
 *
 * @author chaoxin.lu
 */
public class RedisSinkTask extends RateLimitSinkTask {

    private static final Logger log = LoggerFactory.getLogger(RedisSinkTask.class);

    private final Pattern pattern = Pattern.compile("(?<hash>\\{.*\\})");
    private ErrantRecordReporter reporter;

    @Override
    public String version() {
        return Versions.VERSION;
    }

    @Override
    public void start(final Map<String, String> configs) {
        try {
            if (context != null) {
                this.reporter = context.errantRecordReporter();
            }
            super.start(append(configs));
        } catch (ConfigException configException) {
            throw new ConnectException("task configuration error");
        }
    }

    private static Map<String, String> append(Map<String, String> originals) {
        originals.putIfAbsent(RedissonConfig.REDIS_NODES, originals.get(CONNECTION_URL));
        originals.putIfAbsent(RedissonConfig.REDIS_USER, originals.get(CONNECTION_USER));
        originals.putIfAbsent(RedissonConfig.REDIS_PWD, originals.get(CONNECTION_PASSWORD));
        return originals;
    }

    private void syncSingleData(Collection<SinkRecord> records) {
        try {

            for (SinkRecord sr : records) {
                Struct recordValue = (Struct) sr.value();
                Schema schema = recordValue.schema();
                List<Field> fields = schema.fields();
                RBatch batch = this.redisson.createBatch(BatchOptions.defaults()
                        .executionMode(BatchOptions.ExecutionMode.IN_MEMORY_ATOMIC)
                        .retryAttempts(3)
                        .responseTimeout(2, TimeUnit.SECONDS)
                );
                for (Field field : fields) {
                    Schema fieldSchema = field.schema();
                    Object fieldValue = recordValue.get(field);
                    RedisCommand command = RedisCommand.of(fieldSchema.name());
                    if (command == null || fieldValue == null) {
                        continue;
                    }
                    command.getExec().accept((Struct) fieldValue, batch);
                }
                batch.execute();
            }
        } catch (Exception exception) {
            throw new ConnectException("failed to convert sr", exception);
        }
    }

    private void syncClusterData(Collection<SinkRecord> records) {
        int total = records.size();
        Map<String, List<Pair<RedisCommand, Struct>>> group = new HashMap<>(total);
        List<Pair<RedisCommand, Struct>> cache = new ArrayList<>(total);
        Map<RedisCommand, SinkRecord> map = new EnumMap<>(RedisCommand.class);
        try {
            for (SinkRecord sr : records) {
                Struct recordValue = (Struct) sr.value();
                String schemaName = recordValue.schema().name();
                RedisCommand command = RedisCommand.of(schemaName);
                if (command == null) {
                    continue;
                }
                String key = recordValue.getString(RedisConstants.KEY);
                Matcher matcher = pattern.matcher(key);
                if (matcher.find()) {
                    String k = matcher.group("hash");
                    group.computeIfAbsent(k, x -> new ArrayList<>(total))
                            .add(Pair.of(command, recordValue));
                } else {
                    cache.add(Pair.of(command, recordValue));
                }
                map.put(command, sr);
            }
        } catch (Exception exception) {
            throw new ConnectException("failed to convert sr", exception);
        }
        Collection<List<Pair<RedisCommand, Struct>>> values = group.values();
        for (List<Pair<RedisCommand, Struct>> pairs : values) {
            RBatch batch = this.redisson.createBatch(BatchOptions.defaults()
                    .executionMode(BatchOptions.ExecutionMode.IN_MEMORY_ATOMIC)
                    .retryAttempts(3)
                    .responseTimeout(2, TimeUnit.SECONDS)
            );
            try {
                pairs.forEach(c -> c.getKey().getExec().accept(c.getValue(), batch));
                batch.execute();
            } catch (Exception e) {
                if (reporter != null) {
                    pairs.stream()
                            .map(t -> map.get(t.getKey()))
                            .forEach(c -> reporter.report(c, e));
                }
                throw e;
            }
        }

        for (Pair<RedisCommand, Struct> pair : cache) {
            RBatch batch = this.redisson.createBatch(BatchOptions.defaults()
                    .executionMode(BatchOptions.ExecutionMode.IN_MEMORY_ATOMIC)
                    .retryAttempts(3)
                    .responseTimeout(2, TimeUnit.SECONDS)
            );
            try {
                pair.getKey().getExec().accept(pair.getValue(), batch);
                batch.execute();
            } catch (Exception e) {
                if (reporter != null) {
                    reporter.report(map.get(pair.getKey()), e);
                }
                throw e;
            }
        }
    }

    @Override
    public void doPut(Collection<SinkRecord> records) {
        if (records.isEmpty()) {
            return;
        }
        int total = records.size();
        log.debug("Writing {} record(s) to redis", total);
        if (redisClientType == RedisClientType.CLUSTER) {
            syncClusterData(records);
            return;
        }
        syncSingleData(records);
    }

    @Override
    public void stop() {
    }
}
