package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.payment.api.vo.PaymentTaskDispatchResultVO;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentTaskApi {

    R<PaymentTaskDispatchResultVO> expireOpenPaymentOrders(long limit);

    R<PaymentTaskDispatchResultVO> queryProcessingPaymentOrders(long limit);
}
