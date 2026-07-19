package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.payment.api.PaymentObservabilityApi;
import io.mango.payment.api.vo.PaymentObservabilitySnapshotVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "mango-payment", contextId = "paymentObservabilityFeignClient", path = "/payment/observability")
public interface PaymentObservabilityFeignClient extends PaymentObservabilityApi {

    @Override
    @GetMapping("/snapshot")
    R<PaymentObservabilitySnapshotVO> observabilitySnapshot();
}
