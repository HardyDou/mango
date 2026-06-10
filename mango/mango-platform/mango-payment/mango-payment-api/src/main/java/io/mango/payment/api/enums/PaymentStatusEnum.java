package io.mango.payment.api.enums;

/**
 * 支付域通用状态。
 */
public enum PaymentStatusEnum {

    DISABLED(0, "停用"),
    ENABLED(1, "启用");

    private final Integer value;
    private final String label;

    PaymentStatusEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    public Integer getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
