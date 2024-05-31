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
package io.openmessaging.tpch.processing;


import io.openmessaging.tpch.model.TpcHIntermediateResult;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class SingleThreadTpcHStateProvider implements TpcHStateProvider {
    private final Map<String, TpcHIntermediateResult> collectedIntermediateResults = new ConcurrentHashMap<>();
    private final Map<String, TpcHIntermediateResult> collectedReducedResults = new ConcurrentHashMap<>();
    private final Set<String> processedMapMessageIds = new ConcurrentSkipListSet<>();
    private final Map<String, Void> processedIntermediateResults = new ConcurrentHashMap<>();
    private final Map<String, Void> processedReducedResults = new ConcurrentHashMap<>();

    @Override
    public Map<String, TpcHIntermediateResult> getCollectedIntermediateResults() {
        return collectedIntermediateResults;
    }

    @Override
    public Map<String, TpcHIntermediateResult> getCollectedReducedResults() {
        return collectedReducedResults;
    }

    @Override
    public Set<String> getProcessedMapMessageIds() {
        return processedMapMessageIds;
    }

    @Override
    public Map<String, Void> getProcessedIntermediateResults() {
        return processedIntermediateResults;
    }

    @Override
    public Map<String, Void> getProcessedReducedResults() {
        return processedReducedResults;
    }
}
