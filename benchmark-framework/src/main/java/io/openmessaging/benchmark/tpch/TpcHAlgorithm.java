package io.openmessaging.benchmark.tpch;

import io.openmessaging.benchmark.driver.TpcHQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TpcHAlgorithm {

    private static final LocalDate pricingSummaryReportShipDate = LocalDate.of(1998, 1, 12).minusDays(90);

    private static final LocalDate forecastingRevenueChangeMinShipDate = LocalDate.of(1994, 1, 1);
    private static final LocalDate forecastingRevenueChangeMaxShipDate = forecastingRevenueChangeMinShipDate.plusYears(1);

    private static final BigDecimal oneAsBigDecimal = new BigDecimal("1");
    private static final BigDecimal discountLowerBound = new BigDecimal("0.05");
    private static final BigDecimal discountUpperBound = new BigDecimal("0.07");
    private static final BigDecimal quantityLowerBound = new BigDecimal("24.00");

    public static TpcHIntermediateResult applyQueryToChunk(List<TpcHRow> chunk, TpcHQuery query) {
        switch (query) {
            case PricingSummaryReport:
                return applyPricingSummaryReportQueryToChunk(chunk);
            case ForecastingRevenueChange:
                return applyForecastingRevenueChangeReportQueryToChunk(chunk);
            default:
                throw new IllegalArgumentException("Invalid query detected!");
        }
    }

    private static TpcHIntermediateResult applyPricingSummaryReportQueryToChunk(List<TpcHRow> chunk) {
        // TO DO: Perhaps add groupMap to intermediate result.
        HashMap<String, TpcHIntermediateResultGroup> groups = new HashMap<>();
        for (TpcHRow row : chunk) {
            LocalDate shipDate = row.shipDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (pricingSummaryReportShipDate.isBefore(shipDate)) {
                continue;
            }
            String groupId = String.format("%s%s", row.returnFlag, row.lineStatus);
            if (!groups.containsKey(groupId)) {
                TpcHIntermediateResultGroup newGroup = new TpcHIntermediateResultGroup(getPricingSummaryReportQueryAggregates());
                newGroup.identifiers.put("returnFlag", row.returnFlag);
                newGroup.identifiers.put("lineStatus", row.lineStatus);
                groups.put(groupId, newGroup);
            }
            BigDecimal discountedPrice = row.extendedPrice.multiply(oneAsBigDecimal.subtract(row.discount));
            BigDecimal charge = discountedPrice.multiply(oneAsBigDecimal.add(row.tax));
            TpcHIntermediateResultGroup group = groups.get(groupId);
            group.aggregates.put("quantity", ((BigDecimal)group.aggregates.get("quantity")).add(row.quantity));
            group.aggregates.put("basePrice", ((BigDecimal)group.aggregates.get("basePrice")).add(row.extendedPrice));
            group.aggregates.put("discount", ((BigDecimal)group.aggregates.get("discount")).add(row.discount));
            group.aggregates.put("discountedPrice", ((BigDecimal)group.aggregates.get("discountedPrice")).add(discountedPrice));
            group.aggregates.put("charge", ((BigDecimal)group.aggregates.get("charge")).add(charge));
            group.aggregates.put("orderCount", (Long)group.aggregates.get("orderCount") + 1);
        }
        return new TpcHIntermediateResult(new ArrayList<>(groups.values()));
    }

    private static TpcHIntermediateResult applyForecastingRevenueChangeReportQueryToChunk(List<TpcHRow> chunk) {
        HashMap<String, TpcHIntermediateResultGroup> groups = new HashMap<>();
        for (TpcHRow row : chunk) {
            LocalDate shipDate = row.shipDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (shipDate.isBefore(forecastingRevenueChangeMinShipDate)) {
                continue;
            }
            if (shipDate.isEqual(forecastingRevenueChangeMaxShipDate) || shipDate.isAfter(forecastingRevenueChangeMaxShipDate)) {
                continue;
            }
            BigDecimal discount = row.discount;
            if (discount.compareTo(discountLowerBound) < 0 || discount.compareTo(discountUpperBound) > 0 || row.quantity.compareTo(quantityLowerBound) >= 0) {
                continue;
            }
            if (!groups.containsKey("default")) {
                TpcHIntermediateResultGroup newGroup = new TpcHIntermediateResultGroup(getForecastingRevenueChangeQueryAggregates());
                groups.put("default", newGroup);
            }
            TpcHIntermediateResultGroup group = groups.get("default");
            BigDecimal revenue = row.extendedPrice.multiply(row.discount);
            group.aggregates.put("revenue", ((BigDecimal)group.aggregates.get("revenue")).add(revenue));
        }
        return new TpcHIntermediateResult(new ArrayList<>(groups.values()));
    }

    private static Map<String, Number> getPricingSummaryReportQueryAggregates() {
        return new HashMap<String, Number>() {{
            put("quantity", new BigDecimal("0.00"));
            put("basePrice", new BigDecimal("0.00"));
            put("discount", new BigDecimal("0.00"));
            put("discountedPrice", new BigDecimal("0.00"));
            put("charge", new BigDecimal("0.00"));
            put("orderCount", 0L);
        }};
    }

    private static Map<String, Number> getForecastingRevenueChangeQueryAggregates() {
        return new HashMap<String, Number>() {{
            put("revenue", new BigDecimal("0.00"));
        }};
    }
}
