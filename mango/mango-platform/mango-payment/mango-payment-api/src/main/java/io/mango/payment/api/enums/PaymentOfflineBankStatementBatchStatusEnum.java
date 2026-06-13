package io.mango.payment.api.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 线下银行流水导入批次状态。
 */
public enum PaymentOfflineBankStatementBatchStatusEnum {

    /** 已全部匹配。 */
    MATCHED("MATCHED", "已匹配"),

    /** 存在差异。 */
    DIFFERENCE("DIFFERENCE", "存在差异"),

    /** 已全部确认。 */
    CONFIRMED("CONFIRMED", "已确认");

    private final String code;
    private final String label;

    PaymentOfflineBankStatementBatchStatusEnum(String code, String label) {
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
                .map(PaymentOfflineBankStatementBatchStatusEnum::getLabel)
                .orElse(code);
    }

    public static List<PaymentOfflineBankStatementBatchStatusEnum> options() {
        return List.of(values());
    }
}
