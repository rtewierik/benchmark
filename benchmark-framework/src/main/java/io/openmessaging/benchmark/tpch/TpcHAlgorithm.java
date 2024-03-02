package io.openmessaging.benchmark.tpch;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TpcHAlgorithm {

    private static final LocalDate maxShipDate = LocalDate.of(1998, 1, 12).minusDays(90);

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
            if (maxShipDate.isBefore(row.shipDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())) {
                continue;
            }
            String groupId = String.format("%s%s", row.returnFlag, row.lineStatus);
            if (!groups.containsKey(groupId)) {
                TpcHIntermediateResultGroup newGroup  = new TpcHIntermediateResultGroup();
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

    /*
    select
	l_returnflag,
	l_linestatus,
	sum(l_quantity) as sum_qty,
	sum(l_extendedprice) as sum_base_price,
	sum(l_extendedprice * (1 - l_discount)) as sum_disc_price,
	sum(l_extendedprice * (1 - l_discount) * (1 + l_tax)) as sum_charge,
	avg(l_quantity) as avg_qty,
	avg(l_extendedprice) as avg_price,
	avg(l_discount) as avg_disc,
	count(*) as count_order
group by
	l_returnflag,
	l_linestatus
order by
	l_returnflag,
	l_linestatus;
:n -1


     */
    private static TpcHIntermediateResult applyForecastingRevenueChangeReportQueryToChunk(List<TpcHRow> chunk) {

    }
}
