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
package io.openmessaging.benchmark.driver;

public class TpcHInfo {
    public String queryId;
    public TpcHQuery query;
    public TpcHConsumer consumer;
    public Integer index;
    public Integer numberOfMapResults;
    public Integer numberOfReduceResults;

    public TpcHInfo(
        String queryId,
        TpcHQuery query,
        TpcHConsumer consumer,
        Integer index,
        Integer numberOfMapResults,
        Integer numberOfReduceResults
    ) {
        this.queryId = queryId;
        this.query = query;
        this.consumer = consumer;
        this.index = index;
        this.numberOfMapResults = numberOfMapResults;
        this.numberOfReduceResults = numberOfReduceResults;
    }

    @Override
    public String toString() {
        return "TpcHInfo{" +
                "consumer=" + consumer +
                ", numberOfMapResults=" + numberOfMapResults +
                ", numberOfReduceResults=" + numberOfReduceResults +
                '}';
    }
}
