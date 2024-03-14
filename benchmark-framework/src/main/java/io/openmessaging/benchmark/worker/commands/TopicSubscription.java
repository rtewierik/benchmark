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
package io.openmessaging.benchmark.worker.commands;

import io.openmessaging.benchmark.driver.TpcHInfo;

public class TopicSubscription {
    public String topic;
    public String subscription;
    public TpcHInfo info;

    public TopicSubscription() {}

    public TopicSubscription(String topic, String subscription, TpcHInfo info) {
        this.topic = topic;
        this.subscription = subscription;
        this.info = info;
    }

    public TopicSubscription withSubscription(String subscription) {
        TopicSubscription copy = new TopicSubscription();
        copy.topic = this.topic;
        copy.subscription = subscription;
        copy.info = info;
        return copy;
    }

    @Override
    public String toString() {
        return "TopicSubscription{" +
                "topic='" + topic + '\'' +
                ", subscription='" + subscription + '\'' +
                ", info=" + info +
                '}';
    }
}
