package io.openmessaging.benchmark.tpch;

import java.util.Map;

public class TpcHIntermediateResultGroupDto {
    public Map<String, Object> identifiers;
    public Map<String, Number> aggregates;

    public TpcHIntermediateResultGroupDto() {}

    public TpcHIntermediateResultGroupDto(Map<String, Object> identifiers, Map<String, Number> aggregates) {
        this.identifiers = identifiers;
        this.aggregates = aggregates;
    }
}
