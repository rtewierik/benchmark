package io.openmessaging.benchmark.tpch;

import io.openmessaging.benchmark.driver.TpcHQuery;

import java.util.ArrayList;
import java.util.List;

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

    // TO DO: Break up function into two separate ones working with domain-specific models after refactor to optimize.
    private static TpcHIntermediateResult applyReduceToChunkGeneric(List<TpcHIntermediateResult> chunk) {
        TpcHIntermediateResult result = new TpcHIntermediateResult(new ArrayList<>());
        for (TpcHIntermediateResult intermediateResult : chunk) {
            result.aggregateIntermediateResult(intermediateResult);
        }
        return result;
    }
}
