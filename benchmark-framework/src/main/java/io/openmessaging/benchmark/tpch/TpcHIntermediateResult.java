package io.openmessaging.benchmark.tpch;

import java.util.List;

public class TpcHIntermediateResult {
    public List<TpcHIntermediateResultGroup> groups;

    public TpcHIntermediateResult(List<TpcHIntermediateResultGroup> groups) {
        this.groups = groups;
    }
}
