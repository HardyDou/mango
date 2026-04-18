package io.mango.infra.kv.core.support;

import java.util.regex.Pattern;

/**
 * Normalizes capability keys into the Mango KV namespace.
 */
public class KvKeyNormalizer {

    public static final String CACHE = "cache";
    public static final String LOCK = "lock";
    public static final String COUNTER = "counter";
    public static final String RATE_LIMIT = "rate-limit";
    public static final String IDEMPOTENT = "idempotent";
    public static final String TOKEN = "token";
    public static final String IDGEN = "idgen";
    public static final String JDBC_ID = "jdbc-id";

    private static final String SEPARATOR = ":";
    private static final Pattern SEGMENT_PATTERN = Pattern.compile("[A-Za-z0-9._-]+");

    private final boolean enabled;
    private final String prefix;
    private final String env;
    private final boolean appEnabled;
    private final String app;

    public KvKeyNormalizer(boolean enabled, String prefix, String env, boolean appEnabled, String app) {
        this.enabled = enabled;
        this.prefix = normalizePrefix(prefix);
        this.env = normalizeSegment(env, "env");
        this.appEnabled = appEnabled;
        this.app = appEnabled ? normalizeSegment(app, "app") : null;
    }

    public String normalize(String capability, String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
        if (!enabled) {
            return key;
        }
        String normalizedCapability = normalizeSegment(capability, "capability");
        String trimmedKey = key.trim();
        String root = rootPrefix(normalizedCapability);
        if (trimmedKey.startsWith(root + SEPARATOR)) {
            return trimmedKey;
        }
        return root + SEPARATOR + trimmedKey;
    }

    private String rootPrefix(String capability) {
        StringBuilder builder = new StringBuilder(prefix)
                .append(SEPARATOR)
                .append(env);
        if (appEnabled) {
            builder.append(SEPARATOR).append(app);
        }
        return builder.append(SEPARATOR).append(capability).toString();
    }

    private String normalizePrefix(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("prefix cannot be null or blank");
        }
        String normalized = trimSeparator(value.trim());
        for (String segment : normalized.split(SEPARATOR)) {
            normalizeSegment(segment, "prefix");
        }
        return normalized;
    }

    private String normalizeSegment(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be null or blank");
        }
        String normalized = value.trim();
        if (!SEGMENT_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(name + " contains invalid character: " + value);
        }
        return normalized;
    }

    private String trimSeparator(String value) {
        String normalized = value;
        while (normalized.startsWith(SEPARATOR)) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith(SEPARATOR)) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
