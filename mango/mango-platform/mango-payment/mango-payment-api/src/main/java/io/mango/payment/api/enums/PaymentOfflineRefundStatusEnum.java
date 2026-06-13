package io.mango.payment.api.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 线下退款状态。
 */
public enum PaymentOfflineRefundStatusEnum {

    /** 已提交退款凭证，线下退款完成。 */
    REFUNDED("REFUNDED", "已退款"),

    /** 已关闭。 */
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String label;

    PaymentOfflineRefundStatusEnum(String code, String label) {
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
                .map(PaymentOfflineRefundStatusEnum::getLabel)
                .orElse(code);
    }

    public static List<PaymentOfflineRefundStatusEnum> options() {
        return List.of(values());
    }
}
