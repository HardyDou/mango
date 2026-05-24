package io.mango.payment.api.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class RefreshRefundStatusCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "退款单ID不能为空")
    private Long refundOrderId;
}
