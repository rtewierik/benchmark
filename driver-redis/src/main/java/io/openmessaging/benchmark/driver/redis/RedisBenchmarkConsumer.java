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

import static java.nio.charset.StandardCharsets.UTF_8;

import io.lettuce.core.StreamMessage;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.openmessaging.benchmark.common.EnvironmentConfiguration;
import io.openmessaging.benchmark.driver.BenchmarkConsumer;
import io.openmessaging.benchmark.driver.ConsumerCallback;
import io.openmessaging.benchmark.driver.redis.client.AsyncRedisClient;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.resps.StreamEntry;

public class RedisBenchmarkConsumer implements BenchmarkConsumer {
    private final JedisPool pool;
    private final GenericObjectPool<StatefulRedisClusterConnection<String, String>> lettucePool;
    private final String topic;
    private final String subscriptionName;
    private final String consumerId;
    private final ExecutorService executor;
    private final Future<?> consumerTask;
    private final ConsumerCallback consumerCallback;
    private volatile boolean closing = false;

    public RedisBenchmarkConsumer(
            final String consumerId,
            final String topic,
            final String subscriptionName,
            final JedisPool pool,
            final GenericObjectPool<StatefulRedisClusterConnection<String, String>> lettucePool,
            ConsumerCallback consumerCallback) {
        this.pool = pool;
        this.lettucePool = lettucePool;
        this.topic = topic;
        this.subscriptionName = subscriptionName;
        this.consumerId = consumerId;
        this.executor = Executors.newSingleThreadExecutor();
        this.consumerCallback = consumerCallback;
        Jedis jedis = lettucePool == null ? this.pool.getResource() : null;

        this.consumerTask =
                this.executor.submit(
                        () -> {
                            while (!closing) {
                                try {
                                    Map<String, StreamEntryID> streamQuery =
                                            Collections.singletonMap(this.topic, StreamEntryID.UNRECEIVED_ENTRY);
                                    if (lettucePool != null) {
                                        try (StatefulRedisClusterConnection<String, String> connection =
                                                lettucePool.borrowObject()) {
                                            List<StreamMessage<String, String>> range =
                                                    new AsyncRedisClient(connection.async())
                                                            .xreadGroup(subscriptionName, consumerId, topic)
                                                            .get();
                                            handleClusterRange(range);
                                        }
                                    } else {
                                        List<Map.Entry<String, List<StreamEntry>>> range =
                                                jedis.xreadGroup(
                                                        this.subscriptionName,
                                                        this.consumerId,
                                                        XReadGroupParams.xReadGroupParams().block(0),
                                                        streamQuery);
                                        handleRange(range);
                                    }
                                } catch (Exception e) {
                                    log.error("Failed to read from consumer instance.", e);
                                }
                            }
                        });
    }

    private void handleRange(List<Map.Entry<String, List<StreamEntry>>> range) throws IOException {
        if (range != null) {
            for (Map.Entry<String, List<StreamEntry>> streamEntries : range) {
                for (StreamEntry entry : streamEntries.getValue()) {
                    long timestamp = entry.getID().getTime();
                    byte[] payload = entry.getFields().get("payload").getBytes(UTF_8);
                    consumerCallback.messageReceived(payload, timestamp, this);
                }
            }
        }
    }

    private void handleClusterRange(List<StreamMessage<String, String>> range) throws IOException {
        for (StreamMessage<String, String> message : range) {
            String messageId = message.getId();
            String[] parts = messageId.split("-");
            long timestamp = Long.parseLong(parts[0]);
            byte[] payload = message.getBody().get("payload").getBytes(UTF_8);
            consumerCallback.messageReceived(payload, timestamp, this);
        }
    }

    @Override
    public void close() throws Exception {
        closing = true;
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Attempting to shut down consumer task...");
        }
        consumerTask.cancel(true);
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Attempting to shut down executor...");
        }
        executor.shutdown();
        if (EnvironmentConfiguration.isDebug()) {
            log.info("Attempting to shut down pool...");
        }
        if (pool != null) {
            pool.close();
        }
    }

    private static final Logger log = LoggerFactory.getLogger(RedisBenchmarkDriver.class);
}
