package xyz.kafka.connect.redis.redis;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.jupiter.api.Test;
import xyz.kafka.connect.redis.source.RedisSourceTask;

import java.util.List;
import java.util.Map;

/**
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-07-21
 */
public class RedisSourceTests {

    /**
     * KAFKA_SCHEMA_REGISTRY_SVC_ENDPOINT=https://x-schema-registry.x-k8s.piston.ink
     * SCHEMA_REGISTRY_CLIENT_REST_HEADERS=Authorization: Basic a2Fma2Euc3I6YTJGbWEyRXVjMk5vWlcxaExuSmxaMmx6ZEhKNUNn
     * REDIS_NODES=gzv-dev-redis-1.piston.ink:6379
     * REDIS_PWD=test.redis.123444
     * REDIS_CLIENT_NAME=lcx
     */
    @Test
    public void test() {
        String configText = """
                {
                  "connector.class": "xyz.kafka.connect.redis.sink.RedisSourceConnector",
                  "connection.url": "127.0.0.1:6379",
                  "connection.password": "******",
                  "redis.client.type": "SINGLE",
                  "tasks.max": "4",
                  "name": "rest-sink-szc.jms-error-log-to-dingtalk",
                  "batch.size": "200",
                  
                  "behavior.on.null.values": "ignore",
                  "behavior.on.error": "log",
                  "consumer.override.max.request.size": "4194304",
                  "consumer.override.max.poll.records": "2000",
                  "consumer.override.auto.offset.reset": "latest",
                  "topics": "jms-log",
                  "key.converter": "org.apache.kafka.connect.storage.StringConverter",
                  "value.converter": "xyz.kafka.connector.convert.json.JsonConverter",
                  "value.converter.schemas.enable": "false",
                  "value.converter.decimal.format": "NUMERIC",
                  "value.converter.use.big.decimal.for.floats": "false",
                  "value.converter.schema.gen.date.time.infer.enabled": "false",
                  "value.converter.write.big.decimal.as.plain": "true",
                  "value.converter.cache.schemas.enabled": "false",
                  "value.converter.auto.register.schemas": "false",
                  "value.schema.subject.name": "log.jms_value_json",
                  
                  "transforms": "ValueToKey,Drop,ToStruct,BloomFilter",
                  "transforms.Drop.type": "xyz.kafka.connector.transforms.Drop",
                  "transforms.Drop.condition": "valueSchema.fields.keySet().containsAll(['container_name','service_name','message','level','@timestamp','namespace_name']) && !value.container_name.startsWith('it-ucar-data') && value.level == 'ERROR' && value.namespace_name == 'piston-cloud'",
                  "transforms.Drop.null.handling.mode": "drop",
                  "transforms.ValueToKey.type": "org.apache.kafka.connect.transforms.ValueToKey",
                  "transforms.ValueToKey.fields": "time",
                  "transforms.BloomFilter.type": "xyz.kafka.connector.transforms.BloomFilter",
                  "transforms.BloomFilter.bloom.filter.key": "bf:{rest-sink-szc}.jms-error-log-to-dingtalk",
                  "transforms.BloomFilter.bloom.filter.capacity": "1000000",
                  "transforms.BloomFilter.bloom.filter.error.rate": "0.002",
                  "transforms.BloomFilter.bloom.filter.expire.seconds": "3600",
                  "transforms.ToStruct.field.pairs": "biz_resp:biz_resp,req_params:req_params",
                  "transforms.ToStruct.type": "xyz.kafka.connector.transforms.ToStruct$Value",
                  "transforms.ToStruct.behavior.on.error": "LOG",
                  "transforms.StringIdToStruct.type": "xyz.kafka.connector.transforms.StringIdToStruct",
                  
                  "errors.deadletterqueue.context.headers.enable": "true",
                  "errors.deadletterqueue.topic.name": "kafka_connect_dead_letter_queue",
                  "errors.deadletterqueue.topic.replication.factor": "8",
                  "errors.log.enable": "true",
                  "errors.log.include.messages": "true",
                  "errors.retry.timeout": "600000",
                  "errors.tolerance": "none"
                }
                """;

        RedisSourceTask task = new RedisSourceTask();
        Map<String, String> configs = JSON.parseObject(configText, new TypeReference<>() {
        });
        task.start(configs);
        List<SourceRecord> poll = task.poll();
        System.out.println(poll.size());
    }
}
