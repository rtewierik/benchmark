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

public class TpcHProducerAssignment {
    public final String queryId;
    public final TpcHQuery query;
    public final String sourceDataS3FolderUri;
    public final Integer batchSize;
    public final Integer offset;

    public TpcHProducerAssignment(TpcHArguments command, Integer offset) {
        this.queryId = command.queryId;
        this.query = command.query;
        this.sourceDataS3FolderUri = command.sourceDataS3FolderUri;
        this.batchSize = command.getNumberOfMapResults(offset);
        this.offset = offset;
    }
}
