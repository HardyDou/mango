package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.QueryPaymentRefundOrderCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentRefundOrderStatusVO;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface PaymentRefundOrderApi {

    R<PageResult<PaymentRefundOrderVO>> pageRefundOrders(@Valid PaymentConfigPageQuery query);

    R<PaymentRefundOrderVO> detailRefundOrder(@NotNull(message = "退款订单 ID 不能为空") Long id);

    R<List<PaymentRefundOrderStatusVO>> listRefundOrderStatuses();

    R<PaymentRefundOrderVO> queryRefundOrder(@Valid QueryPaymentRefundOrderCommand command);
}
