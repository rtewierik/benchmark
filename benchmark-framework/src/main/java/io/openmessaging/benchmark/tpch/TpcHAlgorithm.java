package io.openmessaging.benchmark.tpch;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TpcHAlgorithm {

    private static final LocalDate pricingSummaryReportShipDate = LocalDate.of(1998, 1, 12).minusDays(90);
    private static final Map<String, Object> pricingSummaryReportQueryAggregates = new HashMap<String, Object>() {{
        put("quantity", 0.0d);
        put("basePrice", 0.0d);
        put("discount", 0.0d);
        put("discountedPrice", 0.0d);
        put("charge", 0.0d);
        put("orderCount", 0);
    }};

    private static final LocalDate forecastingRevenueChangeMinShipDate = LocalDate.of(1994, 1, 1);
    private static final LocalDate forecastingRevenueChangeMaxShipDate = forecastingRevenueChangeMinShipDate.plusYears(1);
    private static final Map<String, Object> forecastingRevenueChangeQueryAggregates = new HashMap<String, Object>() {{
        put("revenue", 0.0d);
    }};

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
        HashMap<String, TpcHIntermediateResultGroup> groups = new HashMap<>();
        for (TpcHRow row : chunk) {
            LocalDate shipDate = row.shipDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (pricingSummaryReportShipDate.isBefore(shipDate)) {
                continue;
            }
            String groupId = String.format("%s%s", row.returnFlag, row.lineStatus);
            if (!groups.containsKey(groupId)) {
                TpcHIntermediateResultGroup newGroup = new TpcHIntermediateResultGroup(pricingSummaryReportQueryAggregates);
                newGroup.groupIdentifiers.put("returnFlag", row.returnFlag);
                newGroup.groupIdentifiers.put("lineStatus", row.lineStatus);
                groups.put(groupId, newGroup);
            }
            Double discountedPrice = row.extendedPrice * (1 - row.discount);
            Double charge = discountedPrice * (1 + row.tax);
            TpcHIntermediateResultGroup group = groups.get(groupId);
            group.aggregates.put("quantity", (Double)group.aggregates.get("quantity") + row.quantity);
            group.aggregates.put("basePrice", (Double)group.aggregates.get("basePrice") + row.extendedPrice);
            group.aggregates.put("discount", (Double)group.aggregates.get("discount") + row.discount);
            group.aggregates.put("discountedPrice", (Integer)group.aggregates.get("discountedPrice") + discountedPrice);
            group.aggregates.put("charge", (Integer)group.aggregates.get("charge") + charge);
            group.aggregates.put("orderCount", (Integer)group.aggregates.get("orderCount") + 1);
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
            if (shipDate.isAfter(forecastingRevenueChangeMaxShipDate)) {
                continue;
            }
            double discount = row.discount;
            if (discount < 0.05d || discount > 0.07d || row.quantity < 24d) {
                continue;
            }
            if (!groups.containsKey("default")) {
                TpcHIntermediateResultGroup newGroup = new TpcHIntermediateResultGroup(forecastingRevenueChangeQueryAggregates);
                groups.put("default", newGroup);
            }
            TpcHIntermediateResultGroup group = groups.get("default");
            Double revenue = row.extendedPrice * row.discount;
            group.aggregates.put("revenue", (Double)group.aggregates.get("revenue") + revenue);
        }
        return new TpcHIntermediateResult(new ArrayList<>(groups.values()));
    }
}
