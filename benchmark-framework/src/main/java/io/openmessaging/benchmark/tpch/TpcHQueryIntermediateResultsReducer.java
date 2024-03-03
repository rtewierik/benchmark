package io.openmessaging.benchmark.tpch;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TpcHQueryIntermediateResultsReducer {

    public static TpcHIntermediateResult applyReduceToChunk(List<TpcHIntermediateResult> chunk, TpcHQuery query) {
        switch (query) {
            case PricingSummaryReport:
                return applyPricingSummaryReportReduceToChunk(chunk);
            case ForecastingRevenueChange:
                return applyForecastingRevenueChangeReportReduceToChunk(chunk);
            default:
                throw new IllegalArgumentException("Invalid query detected!");
        }
    }

    private static TpcHIntermediateResult applyPricingSummaryReportReduceToChunk(List<TpcHIntermediateResult> chunk) {
        return applyReduceToChunkGeneric(chunk);
    }

    private static TpcHIntermediateResult applyForecastingRevenueChangeReportReduceToChunk(List<TpcHIntermediateResult> chunk) {
        return applyReduceToChunkGeneric(chunk);
    }

    // TODO: Break up function into two separate ones working with domain-specific models after refactor to optimize.
    private static TpcHIntermediateResult applyReduceToChunkGeneric(List<TpcHIntermediateResult> chunk) {
        TpcHIntermediateResult result = new TpcHIntermediateResult(new ArrayList<>());
        for (TpcHIntermediateResult intermediateResult : chunk) {
            for (TpcHIntermediateResultGroup group : intermediateResult.groups) {
                TpcHIntermediateResultGroup existingGroup = result.findGroupByIdentifiers(group.identifiers);
                if (existingGroup == null) {
                    result.groups.add(group);
                } else {
                    Map<String, Number> existingAggregates = existingGroup.aggregates;
                    for (Map.Entry<String, Number> aggregate : group.aggregates.entrySet()) {
                        String key = aggregate.getKey();
                        Number existingAggregate = existingAggregates.get(key);
                        Number additionalAggregate = aggregate.getValue();
                        Number updatedAggregate;
                        if (existingAggregate instanceof Long && additionalAggregate instanceof Long) {
                            updatedAggregate = existingAggregate.longValue() + additionalAggregate.longValue();
                        } else if (existingAggregate instanceof BigDecimal && additionalAggregate instanceof BigDecimal) {
                            updatedAggregate = ((BigDecimal)existingAggregate).add((BigDecimal)additionalAggregate);
                        } else {
                            throw new ArithmeticException("Invalid aggregates detected.");
                        }
                        existingGroup.aggregates.put(key, updatedAggregate);
                    }
                }
            }
        }
        return result;
    }
}
