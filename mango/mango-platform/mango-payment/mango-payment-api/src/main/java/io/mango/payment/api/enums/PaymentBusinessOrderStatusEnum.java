package io.mango.payment.api.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 业务订单状态。
 */
public enum PaymentBusinessOrderStatusEnum {

    /** 待支付。 */
    TO_PAY("TO_PAY", "待支付"),

    /** 支付中。 */
    PAYING("PAYING", "支付中"),

    /** 已支付。 */
    PAID("PAID", "已支付"),

    /** 历史成功状态，展示为已支付。 */
    SUCCESS("SUCCESS", "已支付"),

    /** 已关闭。 */
    CLOSED("CLOSED", "已关闭"),

    /** 退款中。 */
    REFUNDING("REFUNDING", "退款中"),

    /** 部分退款。 */
    PARTIAL_REFUNDED("PARTIAL_REFUNDED", "部分退款"),

    /** 已全额退款。 */
    REFUNDED("REFUNDED", "已退款");

    private final String code;
    private final String label;

    PaymentBusinessOrderStatusEnum(String code, String label) {
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
                .map(PaymentBusinessOrderStatusEnum::getLabel)
                .orElse(code);
    }

    public static List<PaymentBusinessOrderStatusEnum> options() {
        return List.of(TO_PAY, PAYING, PAID, CLOSED, REFUNDING, PARTIAL_REFUNDED, REFUNDED);
    }
}
