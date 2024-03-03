package io.openmessaging.benchmark.tpch;


import java.util.HashMap;
import java.util.Map;

public class TpcHIntermediateResultGroup {
    public Map<String, Object> identifiers = new HashMap<>();
    public Map<String, Number> aggregates;

    public TpcHIntermediateResultGroup(Map<String, Number> aggregates) {
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