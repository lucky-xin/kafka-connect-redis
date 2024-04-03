package xyz.kafka.connect.redis.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saasquatch.jsonschemainferrer.FormatInferrers;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.json.jackson.Jackson;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.json.DecimalFormat;
import org.junit.jupiter.api.Test;
import org.redisson.api.GeoEntry;
import xyz.kafka.connector.redis.RedisCommand;
import xyz.kafka.connector.redis.RedisCommands;
import xyz.kafka.connector.utils.StructUtil;
import xyz.kafka.schema.generator.JsonSchemaGenerator;
import xyz.kafka.serialization.json.JsonData;
import xyz.kafka.serialization.json.JsonDataConfig;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2023-07-17
 */
public class RedisCommandTests {

    @Test
    public void genData() throws Exception {
        Map<String, String> configs = Map.of(
                "decimal.format", DecimalFormat.NUMERIC.name()
        );

        ObjectMapper objectMapper = Jackson.newObjectMapper();
        JsonDataConfig jsonDataConfig = new JsonDataConfig(configs);
        Schema schema = RedisCommands.genSchema();
        JsonData jsonData = new JsonData(new JsonDataConfig(configs));

        JsonSchema jsonSchema = RedisCommands.toJsonSchema(schema, jsonData);
        try (OutputStream out = Files.newOutputStream(Path.of("redis-sink-schema.json"))) {
            out.write(jsonSchema.canonicalString().getBytes(StandardCharsets.UTF_8));
        }

        System.out.println(jsonSchema.canonicalString());
        Schema connectSchema = jsonData.toConnectSchema(jsonSchema, Map.of());
        Struct struct = new Struct(schema);
        struct.put(RedisCommand.SET.name(), RedisCommands.set("set_key", "set_value",
                new RedisCommand.Expiration(RedisCommand.Expiration.Type.EX, 3000), RedisCommand.Condition.NX));
        struct.put(RedisCommand.DEL.name(), RedisCommands.del("del_key"));
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
        struct.put(RedisCommand.HDEL.name(), RedisCommands.hdel("hdel_key", List.of("hmset_field", "hmset_value")));
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
        struct.put(RedisCommand.GEO_ADD.name(), RedisCommands.geoAdd("zrem_range_by_lex_key", Map.of("ddd", new GeoEntry(12.3D, 34.67D, "ddd"))));
        struct.put(RedisCommand.JSON_SET.name(), RedisCommands.jsonSet("json_set", "a", "json content"));
        struct.put(RedisCommand.JSON_ARR_APPEND.name(), RedisCommands.jsonArrAppend("json_arr_append", "a", "json content"));
        struct.put(RedisCommand.JSON_ARR_INSERT.name(), RedisCommands.jsonArrInsert("json_arr_insert", "a", 0, List.of("json arr insert")));
        Map<String, Object> map = StructUtil.fromConnectData(schema, struct, jsonDataConfig);
        JsonNode node = objectMapper.convertValue(map, JsonNode.class);
        JsonSchemaGenerator generator = new JsonSchemaGenerator(true, true, true,
                List.of(FormatInferrers.dateTime(),
                        FormatInferrers.ip(),
                        FormatInferrers.email()));
        ObjectNode schema1 = generator.toSchema(node);
        System.out.println(objectMapper.writeValueAsString(schema1));
        jsonSchema.validate(node);
    }
}
