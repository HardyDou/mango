package io.mango.infra.module.api.diagnostic;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModuleDiagnosticConditionTest {

    @Test
    void evidenceIsDeeplyImmutableAndDetachedFromContributorCollections() {
        List<String> pages = new ArrayList<>(List.of("link/items/index"));
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("pages", pages);
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("runtime", nested);

        ModuleDiagnosticCondition condition = condition(evidence);
        pages.add("link/categories/index");
        nested.put("count", 2);

        @SuppressWarnings("unchecked")
        Map<String, Object> immutableNested = (Map<String, Object>) condition.evidence().get("runtime");
        assertEquals(Map.of("pages", List.of("link/items/index")), immutableNested);
        assertThrows(UnsupportedOperationException.class,
                () -> ((List<Object>) immutableNested.get("pages")).add("changed"));
        assertThrows(UnsupportedOperationException.class, () -> immutableNested.put("changed", true));
    }

    @Test
    void unsupportedOrUnboundedEvidenceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> condition(Map.of("secret", new Object())));
        assertThrows(IllegalArgumentException.class,
                () -> condition(Map.of("text", "x".repeat(1025))));
        assertThrows(IllegalArgumentException.class,
                () -> condition(Map.of("values", java.util.stream.IntStream.range(0, 65).boxed().toList())));
    }

    @Test
    void durationIsBoundedWithoutNarrowingOverflow() {
        assertEquals(0, conditionWithDuration(-1L).durationMs());
        assertEquals(24 * 60 * 60 * 1000, conditionWithDuration(Long.MAX_VALUE).durationMs());
    }

    private ModuleDiagnosticCondition condition(Map<String, Object> evidence) {
        return new ModuleDiagnosticCondition(
                ModuleDiagnosticProfile.INSTALLATION,
                ModuleConditionStatus.PASS,
                true,
                "MODULE_INSTALLED",
                evidence,
                Instant.EPOCH,
                0,
                false);
    }

    private ModuleDiagnosticCondition conditionWithDuration(long durationMs) {
        return new ModuleDiagnosticCondition(
                ModuleDiagnosticProfile.INSTALLATION,
                ModuleConditionStatus.PASS,
                true,
                "MODULE_INSTALLED",
                Map.of(),
                Instant.EPOCH,
                durationMs,
                false);
    }
}
