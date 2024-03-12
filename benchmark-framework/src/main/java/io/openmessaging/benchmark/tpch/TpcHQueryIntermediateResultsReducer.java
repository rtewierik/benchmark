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
