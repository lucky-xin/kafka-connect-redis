# Introduction

The Redis plugin is a collection of connectors that are used to interact with a Redis cluster.

# Sink Connectors

## Redis Sink Connector

The Redis Sink Connector is used to write data from Kafka to a Redis cache.

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




