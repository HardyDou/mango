package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.payment.api.command.QueryRefundOrderCommand;
import io.mango.payment.api.command.RefreshRefundStatusCommand;
import io.mango.payment.api.command.RefundCommand;
import io.mango.payment.api.vo.RefundOrderVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

@Validated
public interface RefundApi {

    R<RefundOrderVO> refund(@Valid RefundCommand command);

    R<RefundOrderVO> queryRefundOrder(@Valid QueryRefundOrderCommand command);

    R<RefundOrderVO> refreshRefundStatus(@Valid RefreshRefundStatusCommand command);
}
