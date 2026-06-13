package io.mango.payment.api.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 线下收款状态。
 */
public enum PaymentOfflineCollectionStatusEnum {

    /** 等待用户转账。 */
    WAITING_TRANSFER("WAITING_TRANSFER", "待转账"),

    /** 用户已提交凭证，等待财务确认。 */
    PENDING_CONFIRM("PENDING_CONFIRM", "待确认到账"),

    /** 财务已确认到账。 */
    CONFIRMED("CONFIRMED", "已确认到账"),

    /** 银行流水批量对账已匹配。 */
    RECONCILED("RECONCILED", "已对账"),

    /** 已过期。 */
    EXPIRED("EXPIRED", "已过期"),

    /** 已关闭。 */
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String label;

    PaymentOfflineCollectionStatusEnum(String code, String label) {
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
                .map(PaymentOfflineCollectionStatusEnum::getLabel)
                .orElse(code);
    }

    public static List<PaymentOfflineCollectionStatusEnum> options() {
        return List.of(values());
    }
}
