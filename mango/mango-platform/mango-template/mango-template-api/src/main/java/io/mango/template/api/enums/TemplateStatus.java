package io.mango.template.api.enums;

/**
 * 模板状态。
 */
public enum TemplateStatus {
    DISABLED(0),
    ENABLED(1);

    private final int value;

    TemplateStatus(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
