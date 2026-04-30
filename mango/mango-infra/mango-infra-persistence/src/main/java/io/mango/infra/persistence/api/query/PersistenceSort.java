package io.mango.infra.persistence.api.query;

import java.io.Serializable;
import java.util.Locale;
import java.util.Set;

/**
 * 持久化排序参数。
 */
public class PersistenceSort implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Set<String> SUPPORTED_DIRECTIONS = Set.of("asc", "desc");

    /**
     * 排序字段。
     */
    private String field;

    /**
     * 排序方向，支持 asc 和 desc。
     */
    private String direction = "desc";

    public String getField() {
        return normalize(field);
    }

    public void setField(String field) {
        this.field = normalize(field);
    }

    public String getDirection() {
        String normalized = normalize(direction);
        if (normalized == null) {
            return "desc";
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        return SUPPORTED_DIRECTIONS.contains(lower) ? lower : "desc";
    }

    public void setDirection(String direction) {
        this.direction = normalize(direction);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
