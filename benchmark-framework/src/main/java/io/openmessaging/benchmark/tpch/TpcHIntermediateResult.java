package io.openmessaging.benchmark.tpch;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class TpcHIntermediateResult {
    public String queryId;
    public int numberOfAggregatedResults;
    public List<TpcHIntermediateResultGroup> groups;
    private final Lock lock = new ReentrantLock();

    public TpcHIntermediateResult() {}

    public TpcHIntermediateResult(List<TpcHIntermediateResultGroup> groups) {
        this.queryId = "default-query-id";
        this.groups = groups;
        this.numberOfAggregatedResults = 1;
    }

    public TpcHIntermediateResult(String queryId, List<TpcHIntermediateResultGroup> groups) {
        this.queryId = queryId;
        this.groups = groups;
        this.numberOfAggregatedResults = 1;
    }

    public void aggregateIntermediateResult(TpcHIntermediateResult intermediateResult) {
        lock.lock();
        try {
            for (TpcHIntermediateResultGroup group : intermediateResult.groups) {
                TpcHIntermediateResultGroup existingGroup = this.findGroupByIdentifiers(group.identifiers);
                if (existingGroup == null) {
                    this.groups.add(group.getClone());
                } else {
                    Map<String, Number> existingAggregates = existingGroup.aggregates;
                    for (Map.Entry<String, Number> aggregate : group.aggregates.entrySet()) {
                        String key = aggregate.getKey();
                        Number existingAggregate = existingAggregates.get(key);
                        Number additionalAggregate = aggregate.getValue();
                        Number updatedAggregate;
                        if (existingAggregate instanceof Long && additionalAggregate instanceof Long) {
                            updatedAggregate = existingAggregate.longValue() + additionalAggregate.longValue();
                        } else if (existingAggregate instanceof BigDecimal && additionalAggregate instanceof BigDecimal) {
                            updatedAggregate = ((BigDecimal)existingAggregate).add((BigDecimal)additionalAggregate);
                        } else {
                            throw new ArithmeticException("Invalid aggregates detected.");
                        }
                        existingGroup.aggregates.put(key, updatedAggregate);
                    }
                }
            }
            this.numberOfAggregatedResults += intermediateResult.numberOfAggregatedResults;
        } finally {
            lock.unlock();;
        }
    }

    public TpcHIntermediateResultGroup findGroupByIdentifiers(Map<String, Object> identifiers) {
        int numIdentifiers = identifiers.size();
        List<Map.Entry<String, Object>> identifiersCollection = identifiers
            .entrySet()
            .stream().collect(Collectors.toList());
        for (TpcHIntermediateResultGroup group : this.groups) {
            if (group.identifiers.size() != numIdentifiers) {
                continue;
            }
            Map<String, Object> groupIdentifiers = group.identifiers;
            boolean isMatchingGroup = true;
            for (Map.Entry<String, Object> identifier : identifiersCollection) {
                String key = identifier.getKey();
                if (!groupIdentifiers.containsKey(key)) {
                    isMatchingGroup = false;
                    break;
                }
                if (!groupIdentifiers.get(key).equals(identifier.getValue())) {
                    isMatchingGroup = false;
                    break;
                }
            }
            if (isMatchingGroup) {
                return group;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "TpcHIntermediateResult{" +
                "groups=" + groups +
                '}';
    }
}
