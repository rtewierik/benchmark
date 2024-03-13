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

import java.util.HashMap;
import java.util.Map;

public class TpcHIntermediateResultGroup {
    public final Map<String, Object> identifiers;
    public final Map<String, Number> aggregates;

    public TpcHIntermediateResultGroup(Map<String, Number> aggregates) {
        this.identifiers = new HashMap<>();
        this.aggregates = aggregates;
    }

    public TpcHIntermediateResultGroup(Map<String, Object> identifiers, Map<String, Number> aggregates) {
        this.identifiers = identifiers;
        this.aggregates = aggregates;
    }

    public TpcHIntermediateResultGroupDto toDto() {
        return new TpcHIntermediateResultGroupDto(this.identifiers, this.aggregates);
    }

    public static TpcHIntermediateResultGroup fromDto(TpcHIntermediateResultGroupDto dto) {
        return new TpcHIntermediateResultGroup(dto.identifiers, dto.aggregates);
    }

    public TpcHIntermediateResultGroup getClone() {
        Map<String, Number> hashMapClone = (Map<String, Number>)((HashMap<String, Number>)aggregates).clone();
        TpcHIntermediateResultGroup clone = new TpcHIntermediateResultGroup(identifiers, hashMapClone);
        return clone;
    }

    @Override
    public String toString() {
        return "TpcHIntermediateResultGroup{" +
                "groupIdentifiers=" + identifiers +
                ", aggregates=" + aggregates +
                '}';
    }
}