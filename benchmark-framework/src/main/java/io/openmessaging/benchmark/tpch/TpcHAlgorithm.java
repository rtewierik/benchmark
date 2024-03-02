package io.openmessaging.benchmark.tpch;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
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
                TpcHIntermediateResultGroup group  = new TpcHIntermediateResultGroup();
                group.groupIdentifiers.put("returnFlag", row.returnFlag);
                group.groupIdentifiers.put("lineStatus", row.lineStatus);
                group.aggregates.put("quantitySum", 0.0d);
                group.aggregates.put("basePriceSum", 0.0d);
                group.aggregates.put("discountedPriceSum", 0.0d);
                group.aggregates.put("chargeSum", 0.0d);
                group.aggregates.put("quantityAverage", 0.0d);
                group.aggregates.put("priceAverage", 0.0d);
                group.aggregates.put("discountAverage", 0.0d);
                group.aggregates.put("orderCount", 0);
                groups.put(groupId, group);
            }
        }
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
