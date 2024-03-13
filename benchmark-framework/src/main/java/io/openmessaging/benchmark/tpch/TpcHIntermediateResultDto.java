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

import java.util.List;
import java.util.stream.Collectors;

public class TpcHIntermediateResultDto {
    public final String queryId;
    public final String batchId;
    public final Integer numberOfAggregatedResults;
    public List<TpcHIntermediateResultGroupDto> groups;

    public TpcHIntermediateResultDto(
        @JsonProperty("queryId") String queryId,
        @JsonProperty("batchId") String batchId,
        @JsonProperty("numberOfAggregatedResults") Integer numberOfAggregatedResults,
        @JsonProperty("groups") List<TpcHIntermediateResultGroup> groups) {
        this.queryId = queryId;
        this.batchId = batchId;
        this.numberOfAggregatedResults = numberOfAggregatedResults;
        this.groups = groups.stream().map(TpcHIntermediateResultGroup::toDto).collect(Collectors.toList());
    }
}
