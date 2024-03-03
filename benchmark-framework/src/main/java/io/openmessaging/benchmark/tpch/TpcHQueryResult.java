package io.openmessaging.benchmark.tpch;

import java.util.ArrayList;
import java.util.List;

public class TpcHQueryResult {

    public List<TpcHQueryResultRow> rows = new ArrayList<>();

    @Override
    public String toString() {
        return "TpcHQueryResult{" +
                "rows=" + rows +
                '}';
    }
}
