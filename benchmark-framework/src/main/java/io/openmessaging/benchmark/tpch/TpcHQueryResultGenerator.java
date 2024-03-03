package io.openmessaging.benchmark.tpch;

import java.util.Map;

public class TpcHQueryResultGenerator {

    public static TpcHQueryResult generateResult(TpcHIntermediateResult intermediateResult, TpcHQuery query) {
        switch (query) {
            case PricingSummaryReport:
                return generatePricingSummaryReportResult(intermediateResult);
            case ForecastingRevenueChange:
                return generateForecastingRevenueChangeResult(intermediateResult);
            default:
                throw new IllegalArgumentException("Invalid query detected!");
        }
    }

    private static TpcHQueryResult generatePricingSummaryReportResult(TpcHIntermediateResult intermediateResult) {
        TpcHQueryResult result = new TpcHQueryResult();
        for (TpcHIntermediateResultGroup group : intermediateResult.groups) {
            TpcHQueryResultRow row = new TpcHQueryResultRow();
            row.columns.putAll(group.identifiers);
            Map<String, Number> aggregates = group.aggregates;
            Double quantity = aggregates.get("quantity").doubleValue();
            Double basePrice = aggregates.get("basePrice").doubleValue();
            Double discount = aggregates.get("discount").doubleValue();
            Double discountedPrice = aggregates.get("discountedPrice").doubleValue();
            Double charge = aggregates.get("charge").doubleValue();
            Long orderCount = aggregates.get("orderCount").longValue();
            Double averageQuantity = quantity / orderCount;
            Double averagePrice = basePrice / orderCount;
            Double averageDiscount = discount / orderCount;
            row.columns.put("quantitySum", quantity);
            row.columns.put("basePriceSum", basePrice);
            row.columns.put("discountedPriceSum", discountedPrice);
            row.columns.put("chargeSum", charge);
            row.columns.put("averageQuantity", averageQuantity);
            row.columns.put("averagePrice", averagePrice);
            row.columns.put("averageDiscount", averageDiscount);
            row.columns.put("orderCount", orderCount);
            result.rows.add(row);
        }
        return result;
    }

    private static TpcHQueryResult generateForecastingRevenueChangeResult(TpcHIntermediateResult intermediateResult) {
        TpcHQueryResult result = new TpcHQueryResult();
        for (TpcHIntermediateResultGroup group : intermediateResult.groups) {
            TpcHQueryResultRow row = new TpcHQueryResultRow();
            row.columns.putAll(group.identifiers);
            row.columns.putAll(group.aggregates);
        }
        return result;
    }
}
