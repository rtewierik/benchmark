package io.openmessaging.benchmark.tpch;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
            BigDecimal quantity = (BigDecimal)aggregates.get("quantity");
            BigDecimal basePrice = (BigDecimal)aggregates.get("basePrice");
            BigDecimal discount = (BigDecimal)aggregates.get("discount");
            BigDecimal discountedPrice = (BigDecimal)aggregates.get("discountedPrice");
            BigDecimal charge = (BigDecimal)aggregates.get("charge");
            Long orderCount = aggregates.get("orderCount").longValue();
            BigDecimal orderCountAsDecimal = new BigDecimal(orderCount);
            BigDecimal averageQuantity = quantity.divide(orderCountAsDecimal, RoundingMode.HALF_UP);
            BigDecimal averagePrice = basePrice.divide(orderCountAsDecimal, RoundingMode.HALF_UP);
            BigDecimal averageDiscount = discount.divide(orderCountAsDecimal, RoundingMode.HALF_UP);
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
            result.rows.add(row);
        }
        return result;
    }
}
