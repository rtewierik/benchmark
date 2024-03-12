package io.openmessaging.benchmark.tpch;

import java.util.List;
import java.util.stream.Collectors;

public class TpcHIntermediateResultDto {
    public String queryId;
    public String batchId;
    public int numberOfAggregatedResults;
    public List<TpcHIntermediateResultGroupDto> groups;

    public TpcHIntermediateResultDto() {}

    public TpcHIntermediateResultDto(String queryId, String batchId, int numberOfAggregatedResults, List<TpcHIntermediateResultGroup> groups) {
        this.queryId = queryId;
        this.batchId = batchId;
        this.numberOfAggregatedResults = numberOfAggregatedResults;
        this.groups = groups.stream().map(TpcHIntermediateResultGroup::toDto).collect(Collectors.toList());
    }
}
