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

import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.openmessaging.benchmark.driver.BenchmarkProducer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.XAddParams;

public class RedisBenchmarkProducer implements BenchmarkProducer {
    private final JedisPool pool;
    private final GenericObjectPool<StatefulRedisClusterConnection<String, String>> lettucePool;
    private final String rmqTopic;
    private final XAddParams xaddParams;

    public RedisBenchmarkProducer(
            final JedisPool pool,
            final GenericObjectPool<StatefulRedisClusterConnection<String, String>> lettucePool,
            final String rmqTopic) {
        this.pool = pool;
        this.lettucePool = lettucePool;
        this.rmqTopic = rmqTopic;
        this.xaddParams = redis.clients.jedis.params.XAddParams.xAddParams();
    }

    @Override
    public CompletableFuture<Void> sendAsync(final Optional<String> key, final byte[] payload)
            throws Exception {
        Map<String, String> data = new HashMap<>();
        data.put("payload", new String(payload, UTF_8));

        if (key.isPresent()) {
            data.put("key", key.toString());
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        if (lettucePool != null) {
            try (StatefulRedisClusterConnection<String, String> connection = lettucePool.borrowObject()) {
                connection
                        .async()
                        .xadd(this.rmqTopic, data)
                        .thenRun(() -> future.complete(null))
                        .exceptionally(
                                ex -> {
                                    log.error("Exception occurred while sending xadd command", ex);
                                    future.completeExceptionally(ex);
                                    return null;
                                });
                return future;
            }
        }
        try (Jedis jedis = this.pool.getResource()) {
            jedis.xadd(this.rmqTopic, data, this.xaddParams);
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public void close() throws Exception {
        // Close in Driver
    }

    private static final Logger log = LoggerFactory.getLogger(RedisBenchmarkProducer.class);
}
