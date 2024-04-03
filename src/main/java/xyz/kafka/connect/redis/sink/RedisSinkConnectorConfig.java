package xyz.kafka.connect.redis.sink;

import org.apache.kafka.common.config.ConfigDef;
import xyz.kafka.connect.redis.AbstractRedisConnectorConfig;

import java.util.Map;

/**
 * @author chaoxin.lu
 */
public class RedisSinkConnectorConfig extends AbstractRedisConnectorConfig {

    private static ConfigDef configDef() {
        return CONFIG_DEF;
    }

    /**
     * Configuration for Redis Sink.
     *
     * @param originals configurations.
     */
    public RedisSinkConnectorConfig(Map<String, String> originals) {
        super(configDef(), originals);
    }

}
