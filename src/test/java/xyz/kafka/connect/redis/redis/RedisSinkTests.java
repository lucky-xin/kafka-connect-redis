package xyz.kafka.connect.redis.redis;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;
import org.redisson.api.GeoEntry;
import xyz.kafka.connect.redis.sink.RedisSinkTask;
import xyz.kafka.connector.redis.RedisCommand;
import xyz.kafka.connector.redis.RedisCommands;
import xyz.kafka.utils.StringUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author luchaoxin
 * @version V 1.0
 * @since 2023-07-21
 */
class RedisSinkTests {

    @Test
    void pollDataTest() throws IOException {
        String configText = """
                {
                  "connector.class": "xyz.kafka.connect.redis.sink.RedisSinkConnector",
                  "connection.url": "127.0.0.1:6379",
                  "connection.password": "******",
                  "redis.client.type": "STANDALONE",
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

        RedisSinkTask task = new RedisSinkTask();
        Map<String, String> configs = JSON.parseObject(configText, new TypeReference<>() {
        });
        task.start(configs);
        int count = 20;
        Collection<SinkRecord> records = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            Struct struct = randomStruct();
            SinkRecord sr = new SinkRecord(
                    "lcx",
                    1,
                    Schema.STRING_SCHEMA,
                    StringUtil.getUuid(),
                    struct.schema(),
                    struct,
                    i
            );
            records.add(sr);
        }
        task.put(records);
    }

    private Struct randomStruct() {
        Schema schema = RedisCommands.genSchema();
        Struct struct = new Struct(schema);
        struct.put(RedisCommand.SET.name(), RedisCommands.set("set_key", "set_value",
                new RedisCommand.Expiration(RedisCommand.Expiration.Type.EX, 3000), RedisCommand.Condition.NX));
        struct.put(RedisCommand.DEL.name(), RedisCommands.del("set_key"));
        struct.put(RedisCommand.SETNX.name(), RedisCommands.setnx("setex_key", "setex_value"));
        struct.put(RedisCommand.INCR.name(), RedisCommands.incr("incr_key"));
        struct.put(RedisCommand.INCRBY.name(), RedisCommands.incrby("incrby_key", 10L));
        struct.put(RedisCommand.INCRBYFLOAT.name(), RedisCommands.incrbyfloat("incrbyfloat_key", 10D));
        struct.put(RedisCommand.DECR.name(), RedisCommands.decr("decr_key"));
        struct.put(RedisCommand.DECRBY.name(), RedisCommands.decrby("decrby_key", 10L));
        struct.put(RedisCommand.PERSIST.name(), RedisCommands.persist("persist_key"));
        struct.put(RedisCommand.EXPIRE.name(), RedisCommands.expire("expire_key", 30L));
        struct.put(RedisCommand.EXPIREAT.name(), RedisCommands.expireat("expireat_key", System.currentTimeMillis()));
        struct.put(RedisCommand.PTTL.name(), RedisCommands.pttl("pttl_key"));
        struct.put(RedisCommand.HSET.name(), RedisCommands.hset("hset_key", "hset_field", "hset_value"));
        struct.put(RedisCommand.HSETNX.name(), RedisCommands.hsetnx("hsetnx_key", "hsetnx_field", "hsetnx_value"));
        struct.put(RedisCommand.HMSET.name(), RedisCommands.hmset("hmset_key", Map.of("hmset_field", "hmset_value")));
        struct.put(RedisCommand.HDEL.name(), RedisCommands.hdel("hmset_key", List.of("hmset_field", "hmset_value")));
        struct.put(RedisCommand.HINCRBY.name(), RedisCommands.hincrby("hincrby_key", "HINCRBY_field", 100L));
        struct.put(RedisCommand.HINCRBYFLOAT.name(), RedisCommands.hincrbyfloat("hincrbyfloat_key", "HINCRBY_field", 100D));
        struct.put(RedisCommand.LREM.name(), RedisCommands.lrem("lrem_key", "lrem_field", 100L));
        struct.put(RedisCommand.LPUSH.name(), RedisCommands.lpush("lpush_key", List.of("lrem_field")));
        struct.put(RedisCommand.LPOP.name(), RedisCommands.lpop("lpush_key", 1));
        struct.put(RedisCommand.RPUSH.name(), RedisCommands.rpush("rpush_key", List.of("lrem_field")));
        struct.put(RedisCommand.RPOP.name(), RedisCommands.rpop("rpush_key", 1));
        struct.put(RedisCommand.SADD.name(), RedisCommands.sadd("sadd_key", List.of("lrem_field")));
        struct.put(RedisCommand.SREM.name(), RedisCommands.srem("srem_key", List.of("lrem_field")));
        struct.put(RedisCommand.ZADD.name(), RedisCommands.zadd("zadd_key", "zadd", 90D));
        struct.put(RedisCommand.ZINCRBY.name(), RedisCommands.zincrby("zincrby_key", "zadd", 90D));
        struct.put(RedisCommand.ZREM.name(), RedisCommands.zrem("zincrby_key", List.of("zadd")));
        struct.put(RedisCommand.ZREM_RANGE_BY_SCORE.name(), RedisCommands.zremRangeByScore("zrem_range_by_lex_key", 0D, 100D));
        struct.put(RedisCommand.ZREM_RANGE_BY_INDEX.name(), RedisCommands.zremRangeByIndex("zrem_range_by_lex_key", 0, 100));
//        struct.put(RedisCommand.GEO_ADD.name(), RedisCommands.geoAdd("zrem_range_by_lex_key", Map.of("ddd", new GeoEntry(12.3D, 34.67D, "ddd"))));
//        struct.put(RedisCommand.JSON_SET.name(), RedisCommands.jsonSet("json_set", "a", """
//                {"uname: "nix", "age": 18}
//                """));
//        struct.put(RedisCommand.JSON_ARR_APPEND.name(), RedisCommands.jsonArrAppend("json_arr_append", "a", """
//                {"uname: "lcx", "age": 18}
//                """));
//        struct.put(RedisCommand.JSON_ARR_INSERT.name(), RedisCommands.jsonArrInsert("json_arr_insert", "a", 0, List.of(
//                """
//                        {"uname: "lcx", "age": 18}
//                        """,
//                """
//                        {"uname: "lcx", "age": 20}
//                        """
//        )));
        return struct;
    }


}
