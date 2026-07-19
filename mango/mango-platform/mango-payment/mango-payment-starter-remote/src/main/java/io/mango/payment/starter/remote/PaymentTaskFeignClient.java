package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.payment.api.PaymentTaskApi;
import io.mango.payment.api.vo.PaymentTaskDispatchResultVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mango-payment", contextId = "paymentTaskFeignClient", path = "/payment/tasks")
public interface PaymentTaskFeignClient extends PaymentTaskApi {

    @Override
    @PostMapping("/expire-open-orders")
    R<PaymentTaskDispatchResultVO> expireOpenPaymentOrders(
            @RequestParam(value = "limit", defaultValue = "20") long limit);

    @Override
    @PostMapping("/query-processing-orders")
    R<PaymentTaskDispatchResultVO> queryProcessingPaymentOrders(
            @RequestParam(value = "limit", defaultValue = "20") long limit);
}
