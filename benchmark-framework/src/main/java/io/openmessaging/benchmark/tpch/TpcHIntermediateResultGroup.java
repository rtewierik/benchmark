package io.openmessaging.benchmark.tpch;


import java.util.HashMap;
import java.util.Map;

public class TpcHIntermediateResultGroup {
    public Map<String, Object> groupIdentifiers = new HashMap<>();
    public Map<String, Object> aggregates = new HashMap<String, Object>() {{
        put("quantity", 0.0d);
        put("basePrice", 0.0d);
        put("discount", 0.0d);
        put("discountedPrice", 0.0d);
        put("charge", 0.0d);
        put("orderCount", 0);
    }};
}