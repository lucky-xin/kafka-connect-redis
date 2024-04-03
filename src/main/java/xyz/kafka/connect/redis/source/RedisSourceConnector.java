package xyz.kafka.connect.redis.source;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import xyz.kafka.connect.redis.Versions;
import xyz.kafka.connector.config.RedissonConfig;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Entry point for Kafka Connect Redis Sink.
 *
 * @author chaoxin.lu
 */
public class RedisSourceConnector extends SinkConnector {

    private Map<String, String> props;

    @Override
    public String version() {
        return Versions.VERSION;
    }

    @Override
    public void start(final Map<String, String> props) {
        this.props = props;
    }

    @Override
    public Class<? extends Task> taskClass() {
        return RedisSourceTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(final int maxTasks) {
        return Collections.nCopies(maxTasks, props);
    }

    @Override
    public void stop() {
        // Do nothing
    }

    @Override
    public ConfigDef config() {
        return RedissonConfig.CONFIG_DEF;
    }
}
