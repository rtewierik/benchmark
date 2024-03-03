package io.openmessaging.benchmark.tpch;

import java.util.HashMap;
import java.util.Map;

public class TpcHQueryResultRow {
    public Map<String, Object> columns = new HashMap<>();

    @Override
    public String toString() {
        return "TpcHQueryResultRow{" +
                "columns=" + columns +
                '}';
    }
}
