package xyz.kafka.connect.redis.source;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.text.StrPool;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import xyz.kafka.connect.redis.AbstractRedisConnectorConfig;
import xyz.kafka.connector.validator.Validators;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author chaoxin.lu
 */
public class RedisSourceConnectorConfig extends AbstractRedisConnectorConfig {
    public static final String TOPIC_CONFIG = "topic";
    public static final String REDIS_VALUES = "redis.values";
    public static final String KEY_SCAN_KEY_AND_PATTERNS_CONFIG = "redis.scan.key_and_patterns";

    public final List<Pair<String, String>> keyAndPatterns;
    public final List<RedisValue> redisValues;
    public final String topic;
    public static final String SCAN_POINTER_LATEST = "-1";
    public static final String SCAN_POINTER_START = "0";

    public static final Schema VALUE_SCHEMA = SchemaBuilder.struct()
            .field(RedisValue.STRING.getName(), RedisValue.STRING.getSchema())
            .field(RedisValue.HASH.getName(), RedisValue.HASH.getSchema())
            .field(RedisValue.STREAM.getName(), RedisValue.STREAM.getSchema())
            .field(RedisValue.LIST.getName(), RedisValue.LIST.getSchema())
            .field(RedisValue.SET.getName(), RedisValue.SET.getSchema())
            .field(RedisValue.ZSET.getName(), RedisValue.ZSET.getSchema())
            .name("xyz.redis.source.data.value")
            .build();

    public static final Schema KEY_SCHEMA = SchemaBuilder.struct()
            .field("name", Schema.STRING_SCHEMA)
            .field("key.pattern", Schema.STRING_SCHEMA)
            .field("cursor", Schema.INT32_SCHEMA)
            .name("xyz.redis.source.data.key")
            .build();


    private static ConfigDef configDef() {
        return CONFIG_DEF
                .define(
                        KEY_SCAN_KEY_AND_PATTERNS_CONFIG,
                        ConfigDef.Type.LIST,
                        Collections.emptyList(),
                        Validators.nonEmptyList(),
                        ConfigDef.Importance.MEDIUM,
                        "scan key and pattern list")
                .define(
                        REDIS_VALUES,
                        ConfigDef.Type.LIST,
                        Collections.emptyList(),
                        Validators.nonEmptyList(),
                        ConfigDef.Importance.MEDIUM,
                        "redis values")
                ;
    }

    /**
     * Configuration for Redis Sink.
     *
     * @param originals configurations.
     */
    public RedisSourceConnectorConfig(Map<String, String> originals) {
        super(configDef(), originals);
        this.topic = getString(TOPIC_CONFIG);
        this.keyAndPatterns = getList(KEY_SCAN_KEY_AND_PATTERNS_CONFIG)
                .stream()
                .map(t -> {
                    String[] split = t.split(StrPool.COLON);
                    return Pair.of(split[0], split[1].strip());
                }).toList();
        this.redisValues = getList(REDIS_VALUES)
                .stream()
                .map(RedisValue::valueOf)
                .toList();
    }

}
