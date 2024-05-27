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
import io.openmessaging.benchmark.driver.BenchmarkConsumer;
import io.openmessaging.benchmark.driver.BenchmarkDriver;
import io.openmessaging.benchmark.driver.BenchmarkProducer;
import io.openmessaging.benchmark.driver.ConsumerCallback;
import io.openmessaging.benchmark.driver.redis.client.RedisClientConfig;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.apache.bookkeeper.stats.StatsLogger;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public class RedisBenchmarkDriver implements BenchmarkDriver {
    JedisPool jedisPool;
    private RedisClientConfig clientConfig;

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
        if (jedisPool == null) {
            setupJedisConn();
        }
        return CompletableFuture.completedFuture(new RedisBenchmarkProducer(jedisPool, topic));
    }

    @Override
    public CompletableFuture<BenchmarkConsumer> createConsumer(
            final String topic, final String subscriptionName, final ConsumerCallback consumerCallback) {
        String consumerId = "consumer-" + getRandomString();
        if (jedisPool == null) {
            setupJedisConn();
        }
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.xgroupCreate(topic, subscriptionName, null, true);
        } catch (Exception e) {
            log.info("Failed to create consumer instance.", e);
        }
        return CompletableFuture.completedFuture(
                new RedisBenchmarkConsumer(
                        consumerId, topic, subscriptionName, jedisPool, consumerCallback));
    }

    private void setupJedisConn() {
        GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(this.clientConfig.jedisPoolMaxTotal);
        poolConfig.setMaxIdle(this.clientConfig.jedisPoolMaxIdle);
        log.info(
                "Attempting to connect to {}:{} with user {}",
                this.clientConfig.redisHost,
                this.clientConfig.redisPort,
                this.clientConfig.redisUser);
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
            } catch (Throwable ignored) {}
            try {
                Jedis jedis = this.jedisPool.getResource();
                String pattern = "stream:*";
                String cursor = "0";
                ScanResult<String> scanResult = jedis.scan(cursor, new ScanParams().match(pattern));
                log.info("Streams left over: {}", scanResult.getResult().size());
            } catch (Throwable ignored) {}

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
