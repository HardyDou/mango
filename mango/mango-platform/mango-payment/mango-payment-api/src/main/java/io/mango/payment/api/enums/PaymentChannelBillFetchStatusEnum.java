package io.mango.payment.api.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 通道账单获取批次状态。
 */
public enum PaymentChannelBillFetchStatusEnum {

    PROCESSING("PROCESSING", "获取中"),
    SUCCESS("SUCCESS", "获取成功"),
    FAILED("FAILED", "获取失败");

    private final String code;
    private final String label;

    PaymentChannelBillFetchStatusEnum(String code, String label) {
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
                .map(PaymentChannelBillFetchStatusEnum::getLabel)
                .orElse(code);
    }

    public static List<PaymentChannelBillFetchStatusEnum> options() {
        return List.of(values());
    }
}
