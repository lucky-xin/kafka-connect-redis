package xyz.kafka.connect.redis;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import xyz.kafka.connector.config.RedissonConfig;

import java.util.Map;

/**
 * @author AbstractRedisConfig
 */
public class AbstractRedisConnectorConfig extends AbstractConfig {

    public static final String BATCH_SIZE = "batch.size";

    public static final String CONNECTION_URL = "connection.url";
    public static final String CONNECTION_USER = "connection.user";
    public static final String CONNECTION_PASSWORD = "connection.password";

    public static final ConfigDef CONFIG_DEF = RedissonConfig.CONFIG_DEF;

    /**
     * Configuration for Redis Sink.
     *
     * @param originals configurations.
     */
    public AbstractRedisConnectorConfig(ConfigDef configDef, Map<String, String> originals) {
        super(configDef, append(originals));
    }

    private static Map<String, ?> append(Map<String, String> originals) {
        originals.putIfAbsent(RedissonConfig.REDIS_NODES, originals.get(CONNECTION_URL));
        originals.putIfAbsent(RedissonConfig.REDIS_USER, originals.get(CONNECTION_USER));
        originals.putIfAbsent(RedissonConfig.REDIS_PWD, originals.get(CONNECTION_PASSWORD));
        return originals;
    }
}
