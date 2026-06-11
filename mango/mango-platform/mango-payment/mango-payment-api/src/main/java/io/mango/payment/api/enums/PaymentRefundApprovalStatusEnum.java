package io.mango.payment.api.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum PaymentRefundApprovalStatusEnum {

    PENDING("PENDING", "待审核"),
    IN_APPROVAL("IN_APPROVAL", "审批中"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已拒绝");

    private final String code;
    private final String label;

    PaymentRefundApprovalStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static String labelOf(String code) {
        return Arrays.stream(values())
                .filter(status -> status.code.equals(code))
                .map(PaymentRefundApprovalStatusEnum::getLabel)
                .findFirst()
                .orElse(code);
    }

    public static List<PaymentRefundApprovalStatusEnum> options() {
        return List.of(values());
    }
}
