/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openmessaging.tpch.algorithm;


import io.openmessaging.tpch.model.TpcHIntermediateResult;
import io.openmessaging.tpch.model.TpcHIntermediateResultGroup;
import io.openmessaging.tpch.model.TpcHQuery;
import io.openmessaging.tpch.model.TpcHQueryResult;
import io.openmessaging.tpch.model.TpcHQueryResultRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class TpcHQueryResultGenerator {

    public static TpcHQueryResult generateResult(TpcHIntermediateResult intermediateResult) {
        return TpcHQueryResultGenerator.generateResult(intermediateResult, intermediateResult.query);
    }

    public static TpcHQueryResult generateResult(
            TpcHIntermediateResult intermediateResult, TpcHQuery query) {
        switch (query) {
            case PricingSummaryReport:
                return generatePricingSummaryReportResult(intermediateResult);
            case ForecastingRevenueChange:
                return generateForecastingRevenueChangeResult(intermediateResult);
            default:
                throw new IllegalArgumentException("Invalid query detected!");
        }
    }

    private static TpcHQueryResult generatePricingSummaryReportResult(
            TpcHIntermediateResult intermediateResult) {
        TpcHQueryResult result = new TpcHQueryResult();
        for (TpcHIntermediateResultGroup group : intermediateResult.groups) {
            TpcHQueryResultRow row = new TpcHQueryResultRow();
            row.columns.putAll(group.identifiers);
            Map<String, Number> aggregates = group.aggregates;
            BigDecimal quantity = getDecimalValue(aggregates.get("quantity"));
            BigDecimal basePrice = getDecimalValue(aggregates.get("basePrice"));
            BigDecimal discount = getDecimalValue(aggregates.get("discount"));
            BigDecimal discountedPrice = getDecimalValue(aggregates.get("discountedPrice"));
            BigDecimal charge = getDecimalValue(aggregates.get("charge"));
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

    private static BigDecimal getDecimalValue(Number number) {
        try {
            if (number instanceof Double) {
                return BigDecimal.valueOf(number.doubleValue());
            }
            if (number instanceof Long) {
                return BigDecimal.valueOf(number.longValue());
            }
        } catch (NumberFormatException ignored) {
        }
        return (BigDecimal) number;
    }

    private static TpcHQueryResult generateForecastingRevenueChangeResult(
            TpcHIntermediateResult intermediateResult) {
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
