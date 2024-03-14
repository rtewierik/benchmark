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
package io.openmessaging.benchmark.tpch;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class TpcHIntermediateResultGroup {
    public final Map<String, Object> identifiers;
    public final Map<String, Number> aggregates;

    public TpcHIntermediateResultGroup(Map<String, Number> aggregates) {
        this.identifiers = new HashMap<>();
        this.aggregates = aggregates;
    }
    public TpcHIntermediateResultGroup(TpcHIntermediateResultGroup otherGroup) {
        this.identifiers = otherGroup.identifiers;
        this.aggregates = (Map<String, Number>)((HashMap<String, Number>)otherGroup.aggregates).clone();
    }

    public TpcHIntermediateResultGroup(
        @JsonProperty("identifiers") Map<String, Object> identifiers,
        @JsonProperty("aggregates") Map<String, Number> aggregates
    ) {
        this.identifiers = identifiers;
        this.aggregates = aggregates;
    }

    @Override
    public String toString() {
        return "TpcHIntermediateResultGroup{" +
                "groupIdentifiers=" + identifiers +
                ", aggregates=" + aggregates +
                '}';
    }
}