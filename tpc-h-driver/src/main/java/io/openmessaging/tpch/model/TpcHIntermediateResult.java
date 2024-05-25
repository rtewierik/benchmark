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
package io.openmessaging.tpch.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TpcHIntermediateResult {
    public final TpcHQuery query;
    public final String queryId;
    public final String batchId;
    public final Integer chunkIndex;
    public Integer numberOfAggregatedResults;
    public final Integer numberOfMapResults;
    public final Integer numberOfChunks;
    public final List<TpcHIntermediateResultGroup> groups;

    public TpcHIntermediateResult(
            TpcHConsumerAssignment assignment, Map<String, TpcHIntermediateResultGroup> groups) {
        this.query = assignment.query;
        this.queryId = assignment.queryId;
        this.batchId = assignment.batchId;
        this.chunkIndex = assignment.chunkIndex;
        this.numberOfAggregatedResults = 1;
        this.numberOfMapResults = assignment.numberOfMapResults;
        this.numberOfChunks = assignment.numberOfChunks;
        this.groups = new ArrayList<>(groups.values());
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public TpcHIntermediateResult(
            @JsonProperty("query") TpcHQuery query,
            @JsonProperty("queryId") String queryId,
            @JsonProperty("batchId") String batchId,
            @JsonProperty("chunkIndex") Integer chunkIndex,
            @JsonProperty("numberOfAggregatedResults") Integer numberOfAggregatedResults,
            @JsonProperty("numberOfMapResults") Integer numberOfMapResults,
            @JsonProperty("numberOfChunks") Integer numberOfChunks,
            @JsonProperty("groups") List<TpcHIntermediateResultGroup> groups) {
        this.query = query;
        this.queryId = queryId;
        this.batchId = batchId;
        this.chunkIndex = chunkIndex;
        this.numberOfAggregatedResults = numberOfAggregatedResults;
        this.numberOfMapResults = numberOfMapResults;
        this.numberOfChunks = numberOfChunks;
        this.groups = groups;
    }

    public void aggregateIntermediateResult(TpcHIntermediateResult intermediateResult) {
        String batchId = intermediateResult.batchId;
        if (!this.batchId.equals(batchId)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Inconsistent batch ID \"%s\" found relative to \"%s\".", batchId, this.batchId));
        }
        this.aggregateResult(intermediateResult);
    }

    public void aggregateReducedResult(TpcHIntermediateResult reducedResult) {
        this.aggregateResult(reducedResult);
    }

    private void aggregateResult(TpcHIntermediateResult result) {
        String queryId = result.queryId;
        if (!this.queryId.equals(queryId)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Inconsistent query ID \"%s\" found relative to \"%s\".", queryId, this.queryId));
        }
        for (TpcHIntermediateResultGroup group : result.groups) {
            TpcHIntermediateResultGroup existingGroup = this.findGroupByIdentifiers(group.identifiers);
            if (existingGroup == null) {
                this.groups.add(new TpcHIntermediateResultGroup(group));
            } else {
                Map<String, Number> existingAggregates = existingGroup.aggregates;
                for (Map.Entry<String, Number> aggregate : group.aggregates.entrySet()) {
                    String key = aggregate.getKey();
                    Number existingAggregate = existingAggregates.get(key);
                    Number additionalAggregate = aggregate.getValue();
                    Number updatedAggregate;
                    if ((existingAggregate instanceof Long || existingAggregate instanceof Integer)
                            && (additionalAggregate instanceof Long || additionalAggregate instanceof Integer)) {
                        updatedAggregate = existingAggregate.longValue() + additionalAggregate.longValue();
                    } else if ((existingAggregate instanceof Double
                                    || existingAggregate instanceof BigDecimal)
                            && (additionalAggregate instanceof Double
                                    || additionalAggregate instanceof BigDecimal)) {
                        BigDecimal existingAggregateDecimal =
                                existingAggregate instanceof Double
                                        ? BigDecimal.valueOf((Double) existingAggregate)
                                        : (BigDecimal) existingAggregate;
                        BigDecimal additionalAggregateDecimal =
                                additionalAggregate instanceof Double
                                        ? BigDecimal.valueOf((Double) additionalAggregate)
                                        : (BigDecimal) additionalAggregate;
                        updatedAggregate = existingAggregateDecimal.add(additionalAggregateDecimal);
                    } else {
                        throw new ArithmeticException("Invalid aggregates detected.");
                    }
                    existingGroup.aggregates.put(key, updatedAggregate);
                }
            }
        }
        this.numberOfAggregatedResults += result.numberOfAggregatedResults;
    }

    public TpcHIntermediateResultGroup findGroupByIdentifiers(Map<String, Object> identifiers) {
        int numIdentifiers = identifiers.size();
        List<Map.Entry<String, Object>> identifiersCollection = new ArrayList<>(identifiers.entrySet());
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
        return "TpcHIntermediateResult{" + "groups=" + groups + '}';
    }
}
