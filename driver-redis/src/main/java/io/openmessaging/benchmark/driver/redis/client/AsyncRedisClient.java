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
package io.openmessaging.benchmark.driver.redis.client;


import io.lettuce.core.Consumer;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AsyncRedisClient {
    public final RedisAdvancedClusterAsyncCommands<String, String> asyncCommands;

    public AsyncRedisClient(RedisAdvancedClusterAsyncCommands<String, String> asyncCommands) {
        this.asyncCommands = asyncCommands;
    }

    public CompletableFuture<Void> xaddToStream(String topic, Map<String, String> data) {
        return asyncCommands.xadd(topic, data).toCompletableFuture().thenRun(() -> {});
    }

    public RedisFuture<List<StreamMessage<String, String>>> xreadGroup(
            String subscriptionName, String consumerId, String topic) {
        Consumer<String> consumer = Consumer.from(subscriptionName, consumerId);
        XReadArgs readArgs = XReadArgs.Builder.block(0);
        XReadArgs.StreamOffset<String> offset = XReadArgs.StreamOffset.lastConsumed(topic);
        return asyncCommands.xreadgroup(consumer, readArgs, offset);
    }
}
