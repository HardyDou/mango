package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentOrderStatusVO;
import io.mango.payment.api.vo.PaymentOrderSyncStatusVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface PaymentOrderApi {

    R<PageResult<PaymentOrderVO>> pagePaymentOrders(@Valid PaymentConfigPageQuery query);

    R<PaymentOrderVO> detailPaymentOrder(@NotNull(message = "支付订单 ID 不能为空") Long id);

    R<List<PaymentOrderStatusVO>> listPaymentOrderStatuses();

    R<PaymentOrderSyncStatusVO> syncPaymentOrderStatus(@NotBlank(message = "支付订单号不能为空") String payOrderNo);
}
