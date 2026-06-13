package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.CreatePaymentBusinessOrderCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentBusinessOrderStatusVO;
import io.mango.payment.api.vo.PaymentBusinessOrderVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface PaymentBusinessOrderApi {

    R<PageResult<PaymentBusinessOrderVO>> pageBusinessOrders(@Valid PaymentConfigPageQuery query);

    R<PaymentBusinessOrderVO> detailBusinessOrder(@NotNull(message = "业务订单 ID 不能为空") Long id);

    R<List<PaymentBusinessOrderStatusVO>> listBusinessOrderStatuses();

    R<PaymentBusinessOrderVO> createBusinessOrder(@Valid CreatePaymentBusinessOrderCommand command);
}
