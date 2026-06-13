package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.HandlePaymentExceptionOrderCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentExceptionOrderActionVO;
import io.mango.payment.api.vo.PaymentExceptionOrderStatusVO;
import io.mango.payment.api.vo.PaymentExceptionOrderVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface PaymentExceptionOrderApi {

    R<PageResult<PaymentExceptionOrderVO>> pageExceptionOrders(@Valid PaymentConfigPageQuery query);

    R<PaymentExceptionOrderVO> detailExceptionOrder(@NotNull(message = "异常订单 ID 不能为空") Long id);

    R<List<PaymentExceptionOrderStatusVO>> listExceptionOrderStatuses();

    R<List<PaymentExceptionOrderActionVO>> listExceptionOrderActions();

    R<PaymentExceptionOrderVO> handleExceptionOrder(@Valid HandlePaymentExceptionOrderCommand command);
}
