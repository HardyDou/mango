package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.payment.api.vo.PaymentTaskDispatchResultVO;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentTaskApi {

    R<PaymentTaskDispatchResultVO> expireOpenPaymentOrders(
            @Positive(message = "处理数量必须大于 0") long limit);

    R<PaymentTaskDispatchResultVO> queryProcessingPaymentOrders(
            @Positive(message = "处理数量必须大于 0") long limit);
}
