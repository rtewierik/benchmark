package io.openmessaging.tpch.algorithm;


import io.openmessaging.tpch.model.TpcHIntermediateResult;
import io.openmessaging.tpch.model.TpcHQuery;
import java.util.ArrayList;
import java.util.List;

public class TpcHQueryIntermediateResultsReducer {

    public static TpcHIntermediateResult applyReduceToChunk(
            List<TpcHIntermediateResult> chunk, TpcHQuery query) {
        switch (query) {
            case PricingSummaryReport:
                return applyPricingSummaryReportReduceToChunk(chunk);
            case ForecastingRevenueChange:
                return applyForecastingRevenueChangeReportReduceToChunk(chunk);
            default:
                throw new IllegalArgumentException("Invalid query detected!");
        }
    }

    private static TpcHIntermediateResult applyPricingSummaryReportReduceToChunk(
            List<TpcHIntermediateResult> chunk) {
        return applyReduceToChunkGeneric(chunk);
    }

    private static TpcHIntermediateResult applyForecastingRevenueChangeReportReduceToChunk(
            List<TpcHIntermediateResult> chunk) {
        return applyReduceToChunkGeneric(chunk);
    }

    private static TpcHIntermediateResult applyReduceToChunkGeneric(
            List<TpcHIntermediateResult> chunk) {
        TpcHIntermediateResult result =
                new TpcHIntermediateResult(
                        TpcHQuery.PricingSummaryReport, "query-id", "batch-id", 0, 1, 1, 1, new ArrayList<>());
        for (TpcHIntermediateResult intermediateResult : chunk) {
            result.aggregateIntermediateResult(intermediateResult);
        }
        return result;
    }
}