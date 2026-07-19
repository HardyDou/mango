package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.payment.api.MangoPayVirtualPaymentApi;
import io.mango.payment.api.command.CreateMangoPayScenarioControlCommand;
import io.mango.payment.api.command.MangoPayVirtualPaymentCommand;
import io.mango.payment.api.vo.MangoPayVirtualPaymentResultVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "mango-payment", contextId = "mangoPayVirtualPaymentFeignClient", path = "/payment/mango-pay/virtual")
public interface MangoPayVirtualPaymentFeignClient extends MangoPayVirtualPaymentApi {

    @Override
    @PostMapping("/pay")
    R<MangoPayVirtualPaymentResultVO> pay(@RequestBody MangoPayVirtualPaymentCommand command);

    @Override
    @PostMapping("/scenario-controls")
    R<Long> createMangoPayScenarioControl(@RequestBody CreateMangoPayScenarioControlCommand command);
}
