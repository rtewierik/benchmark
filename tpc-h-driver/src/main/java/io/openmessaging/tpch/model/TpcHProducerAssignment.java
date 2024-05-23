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

public class TpcHProducerAssignment {
    public final String queryId;
    public final TpcHQuery query;
    public final String sourceDataS3FolderUri;
    public final Integer batchSize;
    public final Integer producerNumberOfMapResults;
    public final Integer consumerNumberOfMapResults;
    public final Integer numberOfChunks;
    public final Integer offset;

    public TpcHProducerAssignment(TpcHArguments arguments, Integer producerIndex) {
        this.queryId = arguments.queryId;
        this.query = arguments.query;
        this.sourceDataS3FolderUri = arguments.sourceDataS3FolderUri;
        this.batchSize = arguments.getDefaultNumberOfMapResults(arguments.numberOfWorkers);
        // HARDCODED
        this.producerNumberOfMapResults = arguments.getNumberOfMapResults(producerIndex, 3);
        this.consumerNumberOfMapResults =
                arguments.getNumberOfMapResults(producerIndex, arguments.numberOfWorkers);
        this.numberOfChunks = arguments.numberOfChunks;
        this.offset = producerIndex;
    }
}
