package io.openmessaging.benchmark.tpch;


import java.util.HashMap;
import java.util.Map;

public class TpcHIntermediateResultGroup {
    public Map<String, Object> identifiers = new HashMap<>();
    public Map<String, Number> aggregates;

    public TpcHIntermediateResultGroup(Map<String, Number> aggregates) {
        this.aggregates = aggregates;
    }

    protected TpcHIntermediateResultGroup getClone() {
        Map<String, Number> hashMapClone = (Map<String, Number>)((HashMap<String, Number>)aggregates).clone();
        TpcHIntermediateResultGroup clone = new TpcHIntermediateResultGroup(hashMapClone);
        clone.identifiers = identifiers;
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