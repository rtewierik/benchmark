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
package io.openmessaging.benchmark.driver.pulsar;

import static java.util.Collections.unmodifiableList;

import io.openmessaging.benchmark.driver.BenchmarkConsumer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.apache.pulsar.client.api.Consumer;

public class PulsarBenchmarkConsumer implements BenchmarkConsumer {

    private List<Consumer<ByteBuffer>> consumers;

    public PulsarBenchmarkConsumer() {
        this.consumers = unmodifiableList(new ArrayList<>());
    }

    public PulsarBenchmarkConsumer(List<Consumer<ByteBuffer>> consumer) {
        this.consumers = unmodifiableList(consumer);
    }

    public void setConsumers(List<Consumer<ByteBuffer>> consumers) {
        this.consumers = unmodifiableList(consumers);
    }

    @Override
    public void close() throws Exception {
        for (Consumer<?> c : consumers) {
            c.close();
        }
    }
}
