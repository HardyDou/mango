package io.mango.infra.module.api.diagnostic;

import io.mango.common.contract.LocalCapabilityContract;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable, safe-to-expose evidence for one module condition.
 *
 * @param id stable condition identifier
 * @param status observed status
 * @param required whether the selected profile requires this condition
 * @param reasonCode stable machine-readable reason
 * @param evidence redacted evidence with bounded scalar values
 * @param observedAt observation time
 * @param durationMs observation duration
 * @param stale whether the evidence has exceeded its freshness window
 */
@LocalCapabilityContract
public record ModuleDiagnosticCondition(
        String id,
        ModuleConditionStatus status,
        boolean required,
        String reasonCode,
        Map<String, Object> evidence,
        Instant observedAt,
        int durationMs,
        boolean stale) {

    private static final int MAX_DURATION_MS = 24 * 60 * 60 * 1000;
    private static final int MAX_EVIDENCE_VALUES = 256;
    private static final int MAX_CONTAINER_VALUES = 64;
    private static final int MAX_EVIDENCE_DEPTH = 4;
    private static final int MAX_EVIDENCE_KEY_LENGTH = 128;
    private static final int MAX_EVIDENCE_TEXT_LENGTH = 1024;

    public ModuleDiagnosticCondition {
        id = requireText(id, "id");
        status = status == null ? ModuleConditionStatus.UNKNOWN : status;
        reasonCode = requireText(reasonCode, "reasonCode");
        evidence = immutableEvidence(evidence);
        observedAt = observedAt == null ? Instant.now() : observedAt;
        durationMs = Math.clamp(durationMs, 0, MAX_DURATION_MS);
    }

    /**
     * Accepts native timing sources without allowing narrowing overflow into the wire contract.
     */
    public ModuleDiagnosticCondition(
            String id,
            ModuleConditionStatus status,
            boolean required,
            String reasonCode,
            Map<String, Object> evidence,
            Instant observedAt,
            long durationMs,
            boolean stale) {
        this(id, status, required, reasonCode, evidence, observedAt, boundedDuration(durationMs), stale);
    }

    @Override
    public Map<String, Object> evidence() {
        return Map.copyOf(evidence);
    }

    /**
     * Creates a required unknown condition when a profile contributor is absent.
     */
    public static ModuleDiagnosticCondition missingContributor(String conditionId, Instant observedAt) {
        return new ModuleDiagnosticCondition(
                conditionId,
                ModuleConditionStatus.UNKNOWN,
                true,
                "MISSING_CONTRIBUTOR",
                Map.of(),
                observedAt,
                0,
                false);
    }

    private static Map<String, Object> immutableEvidence(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        int[] budget = new int[1];
        return immutableMap(source, 0, budget);
    }

    private static Map<String, Object> immutableMap(
            Map<?, ?> source,
            int depth,
            int[] budget) {
        requireDepth(depth);
        if (source.size() > MAX_CONTAINER_VALUES) {
            throw new IllegalArgumentException("evidence map exceeds the entry limit");
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((rawKey, value) -> {
            if (!(rawKey instanceof String key) || key.isBlank() || key.length() > MAX_EVIDENCE_KEY_LENGTH) {
                throw new IllegalArgumentException("evidence keys must be bounded non-blank strings");
            }
            consume(budget);
            copy.put(key, immutableValue(value, depth + 1, budget));
        });
        return Collections.unmodifiableMap(copy);
    }

    private static Object immutableValue(Object value, int depth, int[] budget) {
        requireDepth(depth);
        if (value instanceof String text) {
            if (text.length() > MAX_EVIDENCE_TEXT_LENGTH) {
                throw new IllegalArgumentException("evidence text exceeds the length limit");
            }
            return text;
        }
        if (value instanceof Boolean) {
            return value;
        }
        if (value instanceof Number number) {
            return immutableNumber(number);
        }
        if (value instanceof Collection<?> collection) {
            if (collection.size() > MAX_CONTAINER_VALUES) {
                throw new IllegalArgumentException("evidence collection exceeds the entry limit");
            }
            List<Object> copy = new ArrayList<>(collection.size());
            for (Object item : collection) {
                consume(budget);
                copy.add(immutableValue(item, depth + 1, budget));
            }
            return Collections.unmodifiableList(copy);
        }
        if (value instanceof Map<?, ?> map) {
            return immutableMap(map, depth, budget);
        }
        throw new IllegalArgumentException("unsupported evidence value type");
    }

    private static Number immutableNumber(Number number) {
        if (number instanceof Byte
                || number instanceof Short
                || number instanceof Integer
                || number instanceof Long) {
            return number;
        }
        if (number instanceof Float decimal && Float.isFinite(decimal)) {
            return decimal;
        }
        if (number instanceof Double decimal && Double.isFinite(decimal)) {
            return decimal;
        }
        throw new IllegalArgumentException("evidence numbers must be finite supported scalars");
    }

    private static void requireDepth(int depth) {
        if (depth > MAX_EVIDENCE_DEPTH) {
            throw new IllegalArgumentException("evidence exceeds the nesting depth limit");
        }
    }

    private static void consume(int[] budget) {
        budget[0]++;
        if (budget[0] > MAX_EVIDENCE_VALUES) {
            throw new IllegalArgumentException("evidence exceeds the total value limit");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static int boundedDuration(long durationMs) {
        return (int) Math.clamp(durationMs, 0, MAX_DURATION_MS);
    }
}
