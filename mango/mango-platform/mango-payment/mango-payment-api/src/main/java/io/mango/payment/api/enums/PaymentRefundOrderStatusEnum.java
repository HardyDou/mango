package io.mango.payment.api.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 退款订单状态。
 */
public enum PaymentRefundOrderStatusEnum {

    /** 已创建。 */
    CREATED("CREATED", "已创建"),

    /** 退款中。 */
    REFUNDING("REFUNDING", "退款中"),

    /** 退款成功。 */
    SUCCESS("SUCCESS", "退款成功"),

    /** 退款失败。 */
    FAILED("FAILED", "退款失败"),

    /** 已关闭。 */
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String label;

    PaymentRefundOrderStatusEnum(String code, String label) {
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
        if ("PROCESSING".equals(code)) {
            return REFUNDING.label;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .map(PaymentRefundOrderStatusEnum::getLabel)
                .orElse(code);
    }

    public static List<PaymentRefundOrderStatusEnum> options() {
        return List.of(values());
    }
}
