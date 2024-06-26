/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openmessaging.benchmark.driver.redis;

import static io.openmessaging.benchmark.common.random.RandomUtils.RANDOM;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.BaseEncoding;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.support.ConnectionPoolSupport;
import io.openmessaging.benchmark.driver.BenchmarkConsumer;
import io.openmessaging.benchmark.driver.BenchmarkDriver;
import io.openmessaging.benchmark.driver.BenchmarkProducer;
import io.openmessaging.benchmark.driver.ConsumerCallback;
import io.openmessaging.benchmark.driver.redis.client.AsyncRedisClient;
import io.openmessaging.benchmark.driver.redis.client.RedisClientConfig;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.bookkeeper.stats.StatsLogger;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public class RedisBenchmarkDriver implements BenchmarkDriver {
    JedisPool jedisPool;
    private static final ClientResources resources = DefaultClientResources.create();

    private RedisClusterClient clusterClient;
    private AsyncRedisClient asyncRedisClient;
    private RedisClientConfig clientConfig;
    private GenericObjectPool<StatefulRedisClusterConnection<String, String>> lettucePool;
    private StatefulRedisClusterConnection<String, String> connection;
    private RedisAdvancedClusterAsyncCommands<String, String> asyncCommands;

    @Override
    public void initialize(final File configurationFile, final StatsLogger statsLogger)
            throws IOException {
        this.clientConfig = readConfig(configurationFile);
    }

    @Override
    public String getTopicNamePrefix() {
        return "redis-openmessaging-benchmark";
    }

    @Override
    public CompletableFuture<TopicInfo> createTopic(final TopicInfo info) {
        return CompletableFuture.completedFuture(info);
    }

    @Override
    public CompletableFuture<BenchmarkProducer> createProducer(final String topic) {
        synchronized (this) {
            if (jedisPool == null && asyncRedisClient == null) {
                setupJedisConn();
            }
        }
        log.info(
                "Creating producer with Jedis pool {} and cluster client {}", jedisPool, asyncRedisClient);
        return CompletableFuture.completedFuture(
                new RedisBenchmarkProducer(jedisPool, lettucePool, topic));
    }

    @Override
    public CompletableFuture<BenchmarkConsumer> createConsumer(
            final String topic, final String subscriptionName, final ConsumerCallback consumerCallback) {
        String consumerId = "consumer-" + getRandomString();
        if (jedisPool == null && asyncRedisClient == null) {
            setupJedisConn();
        }
        if (asyncRedisClient != null) {
            try {
                XReadArgs.StreamOffset<String> offset = XReadArgs.StreamOffset.from(topic, "0");
                return asyncRedisClient
                        .asyncCommands
                        .xgroupCreate(offset, subscriptionName, XGroupCreateArgs.Builder.mkstream(true))
                        .toCompletableFuture()
                        .thenApply(
                                (ignored) ->
                                        new RedisBenchmarkConsumer(
                                                consumerId,
                                                topic,
                                                subscriptionName,
                                                jedisPool,
                                                lettucePool,
                                                consumerCallback));
            } catch (Exception e) {
                log.info("Failed to create consumer instance.", e);
                CompletableFuture<BenchmarkConsumer> future = new CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            }
        }
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.xgroupCreate(topic, subscriptionName, null, true);
        } catch (Exception e) {
            log.info("Failed to create consumer instance.", e);
        }
        return CompletableFuture.completedFuture(
                new RedisBenchmarkConsumer(
                        consumerId, topic, subscriptionName, jedisPool, null, consumerCallback));
    }

    private void setupJedisConn() {
        log.info(
                "Attempting to connect to {}:{} with user {}",
                this.clientConfig.redisHost,
                this.clientConfig.redisPort,
                this.clientConfig.redisUser);
        GenericObjectPoolConfig<StatefulRedisClusterConnection<String, String>> lettucePoolConfig =
                new GenericObjectPoolConfig<>();
        lettucePoolConfig.setMaxTotal(this.clientConfig.poolMaxTotal);
        lettucePoolConfig.setMaxIdle(this.clientConfig.poolMaxIdle);
        if (this.clientConfig.redisNodes != null && !this.clientConfig.redisNodes.isEmpty()) {
            RedisURI redisUri =
                    RedisURI.Builder.redis(this.clientConfig.redisHost).withPort(6379).build();
            clusterClient = RedisClusterClient.create(resources, redisUri);
            this.lettucePool =
                    ConnectionPoolSupport.createGenericObjectPool(
                            () -> clusterClient.connect(), lettucePoolConfig);
            this.connection = clusterClient.connect();
            this.asyncCommands = connection.async();
            this.asyncRedisClient = new AsyncRedisClient(asyncCommands);
            log.info("Created cluster client: {}", this.asyncRedisClient);
            return;
        }
        GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(this.clientConfig.poolMaxTotal);
        poolConfig.setMaxIdle(this.clientConfig.poolMaxIdle);
        if (this.clientConfig.redisPass != null) {
            if (this.clientConfig.redisUser != null) {
                jedisPool =
                        new JedisPool(
                                poolConfig,
                                this.clientConfig.redisHost,
                                this.clientConfig.redisPort,
                                2000,
                                this.clientConfig.redisPass,
                                this.clientConfig.redisUser);
            } else {
                jedisPool =
                        new JedisPool(
                                poolConfig,
                                this.clientConfig.redisHost,
                                this.clientConfig.redisPort,
                                2000,
                                this.clientConfig.redisPass);
            }
        } else {
            jedisPool =
                    new JedisPool(poolConfig, this.clientConfig.redisHost, this.clientConfig.redisPort, 2000);
        }
    }

    @Override
    public void close() throws Exception {
        if (this.asyncRedisClient != null) {
            try {
                String pattern = "stream:*";
                ScanCursor cursor = ScanCursor.INITIAL;
                do {
                    KeyScanCursor<String> scanResult =
                            asyncRedisClient.asyncCommands.scan(cursor, new ScanArgs().match(pattern)).get();
                    List<String> streamKeys = scanResult.getKeys();
                    cursor = ScanCursor.of(scanResult.getCursor());

                    for (String streamKey : streamKeys) {
                        try {
                            asyncRedisClient.asyncCommands.del(streamKey).get();
                        } catch (Exception e) {
                            log.error("Error deleting stream " + streamKey + ": " + e.getMessage());
                        }
                    }
                } while (!cursor.isFinished());
            } catch (Throwable ignored) {
            }
            try {
                String pattern = "stream:*";
                ScanCursor cursor = ScanCursor.INITIAL;
                KeyScanCursor<String> scanResult =
                        asyncRedisClient.asyncCommands.scan(cursor, new ScanArgs().match(pattern)).get();
                log.info("Streams left over: {}", scanResult.getKeys().size());
            } catch (Throwable ignored) {
            }
            this.asyncCommands.shutdown(false);
            if (this.connection.isOpen()) {
                this.connection.close();
            }
            clusterClient.shutdown();
        }
        if (this.jedisPool != null) {
            try {
                Jedis jedis = this.jedisPool.getResource();
                String pattern = "stream:*";
                String cursor = "0";
                do {
                    ScanResult<String> scanResult = jedis.scan(cursor, new ScanParams().match(pattern));
                    List<String> streamKeys = scanResult.getResult();
                    cursor = scanResult.getCursor();

                    for (String streamKey : streamKeys) {
                        try {
                            jedis.del(streamKey);
                        } catch (Exception e) {
                            log.error("Error deleting stream " + streamKey + ": " + e.getMessage());
                        }
                    }
                } while (!"0".equals(cursor));
            } catch (Throwable ignored) {
            }
            try {
                Jedis jedis = this.jedisPool.getResource();
                String pattern = "stream:*";
                String cursor = "0";
                ScanResult<String> scanResult = jedis.scan(cursor, new ScanParams().match(pattern));
                log.info("Streams left over: {}", scanResult.getResult().size());
            } catch (Throwable ignored) {
            }

            this.jedisPool.close();
        }
    }

    private static final ObjectMapper mapper =
            new ObjectMapper(new YAMLFactory())
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static RedisClientConfig readConfig(File configurationFile) throws IOException {
        return mapper.readValue(configurationFile, RedisClientConfig.class);
    }

    private static String getRandomString() {
        byte[] buffer = new byte[5];
        RANDOM.nextBytes(buffer);
        return BaseEncoding.base64Url().omitPadding().encode(buffer);
    }

    private static final Logger log = LoggerFactory.getLogger(RedisBenchmarkDriver.class);
}
