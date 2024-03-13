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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class TpcHIntermediateResult {
    public final String queryId;
    public final String batchId;
    public Integer numberOfAggregatedResults;
    public final List<TpcHIntermediateResultGroup> groups;
    private final Lock lock = new ReentrantLock();

    public TpcHIntermediateResult(
            @JsonProperty("queryId") String queryId,
            @JsonProperty("batchId") String batchId,
            @JsonProperty("numberOfAggregatedResults") Integer numberOfAggregatedResults,
            @JsonProperty("groups") List<TpcHIntermediateResultGroup> groups) {
        this.queryId = queryId;
        this.batchId = batchId;
        this.numberOfAggregatedResults = numberOfAggregatedResults;
        this.groups = groups;
    }

    public static TpcHIntermediateResult fromDto(TpcHIntermediateResultDto dto) {
        return new TpcHIntermediateResult(
                dto.queryId,
                dto.batchId,
                dto.numberOfAggregatedResults,
                dto.groups.stream().map(TpcHIntermediateResultGroup::fromDto).collect(Collectors.toList())
        );
    }

    public TpcHIntermediateResultDto toDto() {
        return new TpcHIntermediateResultDto(this.queryId, this.batchId, this.numberOfAggregatedResults, this.groups);
    }

    public void aggregateIntermediateResult(TpcHIntermediateResult intermediateResult) {
        lock.lock();
        String queryId = intermediateResult.queryId;
        String batchId = intermediateResult.batchId;
        if (!this.queryId.equals(queryId)) {
            throw new IllegalArgumentException(String.format("Inconsistent query ID \"%s\" found relative to \"%s\".", queryId, this.queryId));
        }
        if (!this.batchId.equals(batchId)) {
            throw new IllegalArgumentException(String.format("Inconsistent batch ID \"%s\" found relative to \"%s\".", batchId, this.batchId));
        }
        try {
            for (TpcHIntermediateResultGroup group : intermediateResult.groups) {
                TpcHIntermediateResultGroup existingGroup = this.findGroupByIdentifiers(group.identifiers);
                if (existingGroup == null) {
                    this.groups.add(group.getClone());
                } else {
                    Map<String, Number> existingAggregates = existingGroup.aggregates;
                    for (Map.Entry<String, Number> aggregate : group.aggregates.entrySet()) {
                        String key = aggregate.getKey();
                        Number existingAggregate = existingAggregates.get(key);
                        Number additionalAggregate = aggregate.getValue();
                        Number updatedAggregate;
                        if ((existingAggregate instanceof Long || existingAggregate instanceof Integer) && (additionalAggregate instanceof Long || additionalAggregate instanceof Integer)) {
                            updatedAggregate = existingAggregate.longValue() + additionalAggregate.longValue();
                        } else if ((existingAggregate instanceof Double || existingAggregate instanceof BigDecimal) && (additionalAggregate instanceof Double || additionalAggregate instanceof BigDecimal)) {
                            BigDecimal existingAggregateDecimal = existingAggregate instanceof Double ? BigDecimal.valueOf((Double)existingAggregate) : (BigDecimal) existingAggregate;
                            BigDecimal additionalAggregateDecimal = additionalAggregate instanceof Double ? BigDecimal.valueOf((Double)additionalAggregate) : (BigDecimal) additionalAggregate;
                            updatedAggregate = existingAggregateDecimal.add(additionalAggregateDecimal);
                        } else {
                            throw new ArithmeticException("Invalid aggregates detected.");
                        }
                        existingGroup.aggregates.put(key, updatedAggregate);
                    }
                }
            }
            this.numberOfAggregatedResults += intermediateResult.numberOfAggregatedResults;
        } finally {
            lock.unlock();;
        }
    }

    public TpcHIntermediateResultGroup findGroupByIdentifiers(Map<String, Object> identifiers) {
        int numIdentifiers = identifiers.size();
        List<Map.Entry<String, Object>> identifiersCollection = identifiers
            .entrySet()
            .stream().collect(Collectors.toList());
        for (TpcHIntermediateResultGroup group : this.groups) {
            if (group.identifiers.size() != numIdentifiers) {
                continue;
            }
            Map<String, Object> groupIdentifiers = group.identifiers;
            boolean isMatchingGroup = true;
            for (Map.Entry<String, Object> identifier : identifiersCollection) {
                String key = identifier.getKey();
                if (!groupIdentifiers.containsKey(key)) {
                    isMatchingGroup = false;
                    break;
                }
                if (!groupIdentifiers.get(key).equals(identifier.getValue())) {
                    isMatchingGroup = false;
                    break;
                }
            }
            if (isMatchingGroup) {
                return group;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "TpcHIntermediateResult{" +
                "groups=" + groups +
                '}';
    }
}
