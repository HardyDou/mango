package io.mango.payment.api.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 线下银行流水匹配状态。
 */
public enum PaymentOfflineBankStatementMatchStatusEnum {

    /** 唯一匹配，等待财务确认。 */
    MATCHED_PENDING_CONFIRM("MATCHED_PENDING_CONFIRM", "已匹配待确认"),

    /** 已确认到账。 */
    CONFIRMED("CONFIRMED", "已确认到账"),

    /** 未匹配到线下收款。 */
    UNMATCHED("UNMATCHED", "未匹配"),

    /** 金额不一致。 */
    AMOUNT_MISMATCH("AMOUNT_MISMATCH", "金额不一致"),

    /** 重复流水。 */
    DUPLICATED_STATEMENT("DUPLICATED_STATEMENT", "重复流水"),

    /** 收款单状态不允许确认。 */
    COLLECTION_STATE_INVALID("COLLECTION_STATE_INVALID", "收款单状态不允许");

    private final String code;
    private final String label;

    PaymentOfflineBankStatementMatchStatusEnum(String code, String label) {
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
                .map(PaymentOfflineBankStatementMatchStatusEnum::getLabel)
                .orElse(code);
    }

    public static List<PaymentOfflineBankStatementMatchStatusEnum> options() {
        return List.of(values());
    }
}
