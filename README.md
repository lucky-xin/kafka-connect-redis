# Introduction

The Redis plugin is a collection of connectors that are used to interact with a Redis cluster.

# Sink Connectors

## Redis Sink Connector

The Redis Sink Connector is used to write data from Kafka to a Redis cache.

### Important

This connector expects records from Kafka to have a key and value that are stored as bytes or a string. If your data is
already in Kafka in the format that you want in Redis consider using the ByteArrayConverter or the StringConverter for
this connector. Keep in this does not need to be configured in the worker properties and can be configured at the
connector level. If your data is not sitting in Kafka in the format you wish to persist in Redis consider using a Single
Message Transformation to convert the data to a byte or string representation before it is written to Redis.

### Note

This connector supports deletes. If the record stored in Kafka has a null value, this connector will send a delete with
the corresponding key to Redis.

### Configuration

#### General

##### `connection.url`

The Redis hosts to connect to.

*Importance:* High

*Type:* List

*Default Value:* [127.0.0.1:6379]

##### `redis.client.type`

The client mode to use when interacting with the Redis cluster.

*Importance:* Medium

*Type:* String

*Default Value:* Standalone

*Validator:* Matches: ``STANDALONE``, ``CLUSTER``, ``SENTINEL``, ``MASTER_SLAVE``

##### `redis.client.name`

Setup connection name during connection init CLIENT SETNAME command

*Importance:* Medium

*Type:* String

*Default Value:* kafka connector

*Validator:*

##### `redis.database`

Redis database to connect to.

*Importance:* Medium

*Type:* Int

*Default Value:* 1

##### `redis.user`

use ssl to connect redis.

*Importance:* Medium

*Type:* Boolean

*Default Value:* false

##### `redis.password`

Password used to connect to Redis.

*Importance:* Medium

*Type:* Password

*Default Value:* [hidden]

##### `redis.timeout.ms`

The amount of time in milliseconds before an operation is marked as timed out.

*Importance:* Medium

*Type:* Long

*Default Value:* 10000

*Validator:* [100,...]

##### `redis.idle.connection.timeout.ms`

If pooled connection not used for a <code>timeout</code> time
and current connections amount bigger than minimum idle connections pool size,
then it will closed and removed from pool.

*Importance:* Medium

*Type:* Long

*Default Value:* 10000

*Validator:* [100,...]

##### `redis.connect.timeout.ms`

Timeout during connecting to any Redis server. Default is <code>10000</code> milliseconds.

*Importance:* Medium

*Type:* Long

*Default Value:* 10000

*Validator:* [100,...]

##### `redis.use_ssl`

Flag to determine if SSL is enabled.

*Importance:* Medium

*Type:* Boolean

*Default Value:* false

##### `redis.ssl.provider`

The SSL provider to use.

*Importance:* Low

*Type:* String

*Default Value:* JDK

*Validator:* Matches: ``OPENSSL``, ``JDK``

##### `redis.ssl.keystore.password`

The password for the SSL keystore.

*Importance:* Medium

*Type:* Password

*Default Value:* [hidden]

##### `redis.ssl.keystore.path`

The path to the SSL keystore.

*Importance:* Medium

*Type:* String

##### `redis.ssl.truststore.password`

The password for the SSL truststore.

*Importance:* Medium

*Type:* Password

*Default Value:* [hidden]

##### `redis.ssl.truststore.path`

The path to the SSL truststore.

*Importance:* Medium

*Type:* String

##### `redis.sentinel.master.name`

Master server name used by Redis Sentinel servers and master change monitoring task.

*Importance:* Medium

*Type:* String

*Default Value:*

##### `redis.retry.attempts`

Error will be thrown if Redis command can't be sent to Redis server after <code>retryAttempts</code>.
But if it sent successfully then <code>timeout</code> will be started.

*Importance:* Medium

*Type:* Integer

*Default Value:* 3

##### `redis.retry.attempts`

Defines time interval for another one attempt send Redis command 
if it hasn't been sent already.
Default is <code>1500</code> milliseconds

*Importance:* Medium

*Type:* Integer

*Default Value:* 1500

#### Examples

##### Standalone Example

This configuration is used typically along
with [standalone mode](http://docs.confluent.io/current/connect/concepts.html#standalone-workers).

```properties
name=RedisSinkConnector1
connector.class=com.github.jcustenborder.kafka.connect.redis.RedisSinkConnector
tasks.max=1
topics=< Required Configuration >
```

##### Distributed Example

This configuration is used typically along
with [distributed mode](http://docs.confluent.io/current/connect/concepts.html#distributed-workers).
Write the following json to `connector.json`, configure all of the required values, and use the command below to
post the configuration to one the distributed connect worker(s).

```json
{
  "config": {
    "name": "RedisSinkConnector1",
    "connector.class": "com.github.jcustenborder.kafka.connect.redis.RedisSinkConnector",
    "tasks.max": "1",
    "topics": "< Required Configuration >"
  }
}
```

Use curl to post the configuration to one of the Kafka Connect Workers. Change `http://localhost:8083/` the the endpoint
of
one of your Kafka Connect worker(s).

Create a new instance.

```bash
curl -s -X POST -H 'Content-Type: application/json' --data @connector.json http://localhost:8083/connectors
```

Update an existing instance.

```bash
curl -s -X PUT -H 'Content-Type: application/json' --data @connector.json http://localhost:8083/connectors/TestSinkConnector1/config
```



