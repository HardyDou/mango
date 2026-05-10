package io.mango.system.api.enums;

import java.util.Arrays;

/**
 * 机构生命周期状态。
 */
public enum InstitutionStatus {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用"),
    FROZEN(2, "冻结"),
    ARCHIVED(9, "归档");

    private final int value;
    private final String label;

    InstitutionStatus(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int value() {
        return value;
    }

    public String label() {
        return label;
    }

    public boolean enabled() {
        return this == ENABLED;
    }

    public static InstitutionStatus of(Integer value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.value == value)
                .findFirst()
                .orElse(null);
    }

    public static boolean valid(Integer value) {
        return of(value) != null;
    }
}
