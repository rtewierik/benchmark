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


import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class ConsumerAssignment {
    public List<TopicSubscription> topicsSubscriptions = new ArrayList<>();
    public String experimentId;
    public boolean isTpcH = false;

    private ConsumerAssignment(
            @JsonProperty("topicsSubscriptions") List<TopicSubscription> topicsSubscriptions,
            @JsonProperty("experimentId") String experimentId,
            @JsonProperty("isTpcH") boolean isTpcH) {
        this.topicsSubscriptions = topicsSubscriptions;
        this.isTpcH = isTpcH;
        this.experimentId = experimentId;
    }

    public ConsumerAssignment(String experimentId) {
        this.experimentId = experimentId;
    }

    public ConsumerAssignment(String experimentId, boolean isTpcH) {
        this.isTpcH = isTpcH;
        this.experimentId = experimentId;
    }

    public ConsumerAssignment(ConsumerAssignment assignment) {
        this(new ArrayList<>(), assignment.experimentId, assignment.isTpcH);
    }
}
