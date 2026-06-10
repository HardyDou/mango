package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.InternalApi;
import io.mango.common.result.R;
import io.mango.payment.api.PaymentChannelCallbackApi;
import io.mango.payment.api.command.PaymentChannelCallbackCommand;
import io.mango.payment.api.vo.PaymentChannelCallbackResultVO;
import io.mango.payment.core.service.PaymentChannelCallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/payment/channel-callbacks")
@RequiredArgsConstructor
@Tag(name = "支付通道标准化回调", description = "支付通道适配器验签后提交的标准化回调接口")
public class PaymentChannelCallbackController implements PaymentChannelCallbackApi {

    private final PaymentChannelCallbackService callbackService;

    @Override
    @InternalApi(desc = "支付通道标准化回调")
    @PostMapping
    @Operation(summary = "处理支付通道标准化回调", description = "由具体通道适配器完成验签后调用，推进支付或退款订单状态并触发业务通知")
    public R<PaymentChannelCallbackResultVO> handle(@Valid @RequestBody PaymentChannelCallbackCommand command) {
        return R.ok(callbackService.handle(command));
    }
}
