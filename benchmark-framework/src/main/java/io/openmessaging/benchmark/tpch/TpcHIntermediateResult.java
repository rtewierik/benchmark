package io.openmessaging.benchmark.tpch;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TpcHIntermediateResult {
    public List<TpcHIntermediateResultGroup> groups;

    public TpcHIntermediateResult(List<TpcHIntermediateResultGroup> groups) {
        this.groups = groups;
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
