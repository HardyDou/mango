package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.payment.api.PaymentChannelCallbackApi;
import io.mango.payment.api.command.PaymentChannelCallbackCommand;
import io.mango.payment.api.vo.PaymentChannelCallbackResultVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "mango-payment", contextId = "paymentChannelCallbackFeignClient", path = "/payment/channel-callbacks")
public interface PaymentChannelCallbackFeignClient extends PaymentChannelCallbackApi {

    @Override
    @PostMapping
    R<PaymentChannelCallbackResultVO> handle(@RequestBody PaymentChannelCallbackCommand command);
}
