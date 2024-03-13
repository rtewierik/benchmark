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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.openmessaging.benchmark.driver.TpcHQuery;

public class TpcHConsumerAssignment {
    public final TpcHQuery query;
    public final String queryId;
    public final String batchId;
    public final Integer index;
    public final String sourceDataS3Uri;

    public TpcHConsumerAssignment(
        @JsonProperty("query") TpcHQuery query,
        @JsonProperty("queryId") String queryId,
        @JsonProperty("batchId") String batchId,
        @JsonProperty("index") Integer index,
        @JsonProperty("sourceDataS3Uri") String sourceDataS3Uri) {
        this.query = query;
        this.queryId = queryId;
        this.batchId = batchId;
        this.index = index;
        this.sourceDataS3Uri = sourceDataS3Uri;
    }
}
