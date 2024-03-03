package io.openmessaging.benchmark.tpch;


import java.util.HashMap;
import java.util.Map;

public class TpcHIntermediateResultGroup {
    public Map<String, Object> groupIdentifiers = new HashMap<>();
    public Map<String, Object> aggregates;

    public TpcHIntermediateResultGroup(Map<String, Object> aggregates) {
        this.aggregates = aggregates;
    }

    @Override
    public String toString() {
        return "TpcHIntermediateResultGroup{" +
                "groupIdentifiers=" + groupIdentifiers +
                ", aggregates=" + aggregates +
                '}';
    }
}