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
package io.openmessaging.tpch.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TpcHArguments {
    private static final ThreadLocal<DateFormat> DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss"));
    public final String queryId;
    public final TpcHQuery query;
    public final String sourceDataS3FolderUri;
    public final Integer numberOfChunks;
    public final Integer numberOfReducers;

    public TpcHArguments(
            @JsonProperty("queryId") String queryId,
            @JsonProperty("query") TpcHQuery query,
            @JsonProperty("sourceDataS3FolderUri") String sourceDataS3FolderUri,
            @JsonProperty("numberOfChunks") Integer numberOfChunks,
            @JsonProperty("numberOFReducers") Integer numberOfReducers) {
        this.queryId = queryId;
        this.query = query;
        this.sourceDataS3FolderUri = sourceDataS3FolderUri;
        this.numberOfChunks = numberOfChunks;
        this.numberOfReducers = numberOfReducers;
    }

    public TpcHArguments withQueryIdDate() {
        String date = DATE_FORMAT.get().format(new Date());
        return new TpcHArguments(
                String.format("%s-%s", this.queryId, date),
                this.query,
                this.sourceDataS3FolderUri,
                this.numberOfChunks,
                this.numberOfReducers);
    }

    public int getNumberOfMapResults(int index) {
        int defaultNumberOfIntermediateResults = this.getDefaultNumberOfMapResults();
        int chunksLeft = numberOfChunks - index * defaultNumberOfIntermediateResults;
        if (chunksLeft < 0) {
            return 0;
        }
        return Math.min(chunksLeft, defaultNumberOfIntermediateResults);
    }

    public int getDefaultNumberOfMapResults() {
        return (int) Math.ceil((double) this.numberOfChunks / this.numberOfReducers);
    }
}
