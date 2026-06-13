package io.mango.payment.api.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 支付订单状态。
 */
public enum PaymentOrderStatusEnum {

    /** 已创建。 */
    CREATED("CREATED", "已创建"),

    /** 支付中。 */
    PAYING("PAYING", "支付中"),

    /** 支付成功。 */
    SUCCESS("SUCCESS", "支付成功"),

    /** 支付失败。 */
    FAILED("FAILED", "支付失败"),

    /** 已关闭。 */
    CLOSED("CLOSED", "已关闭"),

    /** 重复成功支付退款中。 */
    DUPLICATE_REFUNDING("DUPLICATE_REFUNDING", "重复支付退款中"),

    /** 重复成功支付已退款。 */
    DUPLICATE_REFUNDED("DUPLICATE_REFUNDED", "重复支付已退款");

    private final String code;
    private final String label;

    PaymentOrderStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static String labelOf(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .map(PaymentOrderStatusEnum::getLabel)
                .orElse(code);
    }

    public static List<PaymentOrderStatusEnum> options() {
        return List.of(values());
    }
}
