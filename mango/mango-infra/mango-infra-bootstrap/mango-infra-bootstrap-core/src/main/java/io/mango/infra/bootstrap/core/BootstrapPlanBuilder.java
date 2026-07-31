package io.mango.infra.bootstrap.core;

import io.mango.infra.bootstrap.api.BootstrapStep;
import io.mango.infra.bootstrap.api.BootstrapStepContributor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BootstrapPlanBuilder {

    private final BootstrapManifestHasher hasher;

    public BootstrapPlanBuilder(BootstrapManifestHasher hasher) {
        this.hasher = Objects.requireNonNull(hasher, "hasher");
    }

    public BootstrapPlan build(String releaseId, String buildRevision,
                               List<BootstrapStepContributor> contributors) {
        Map<String, BootstrapStep> byCode = new LinkedHashMap<>();
        contributors.forEach(contributor -> contributor.contributeSteps().forEach(step -> {
            requireText(step.code(), "Bootstrap step code is required");
            requireText(step.fingerprintMaterial(), "Bootstrap step fingerprint material is required: " + step.code());
            BootstrapStep previous = byCode.putIfAbsent(step.code(), step);
            if (previous != null) {
                throw new IllegalStateException("Duplicate bootstrap step code: " + step.code());
            }
        }));
        byCode.values().forEach(step -> step.dependencies().forEach(dependency -> {
            if (!byCode.containsKey(dependency)) {
                throw new IllegalStateException("Missing bootstrap step dependency: step=" + step.code()
                        + ", dependency=" + dependency);
            }
        }));
        List<BootstrapStep> ordered = topologicalSort(byCode);
        return new BootstrapPlan(hasher.fingerprint(releaseId, buildRevision, ordered), ordered);
    }

    private static List<BootstrapStep> topologicalSort(Map<String, BootstrapStep> byCode) {
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();
        byCode.forEach((code, step) -> {
            List<String> effectiveDependencies = new ArrayList<>(step.dependencies());
            step.optionalDependencies().stream()
                    .filter(byCode::containsKey)
                    .forEach(effectiveDependencies::add);
            indegree.put(code, effectiveDependencies.size());
            effectiveDependencies.forEach(dependency ->
                    dependents.computeIfAbsent(dependency, ignored -> new ArrayList<>()).add(code));
        });
        ArrayDeque<String> ready = new ArrayDeque<>(indegree.entrySet().stream()
                .filter(entry -> entry.getValue() == 0)
                .map(Map.Entry::getKey)
                .sorted()
                .toList());
        List<BootstrapStep> result = new ArrayList<>();
        while (!ready.isEmpty()) {
            String code = ready.removeFirst();
            result.add(byCode.get(code));
            dependents.getOrDefault(code, List.of()).stream().sorted().forEach(dependent -> {
                int remaining = indegree.computeIfPresent(dependent, (ignored, value) -> value - 1);
                if (remaining == 0) {
                    ready.addLast(dependent);
                }
            });
        }
        if (result.size() != byCode.size()) {
            List<String> cycle = indegree.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .map(Map.Entry::getKey)
                    .sorted(Comparator.naturalOrder())
                    .toList();
            throw new IllegalStateException("Cyclic bootstrap step dependencies: " + cycle);
        }
        return result;
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }
}
