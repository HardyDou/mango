package io.mango.payment.starter.controller;

import io.mango.common.result.R;
import io.mango.payment.api.MangoPayVirtualPaymentApi;
import io.mango.payment.api.command.MangoPayVirtualPaymentCommand;
import io.mango.payment.api.vo.MangoPayVirtualPaymentResultVO;
import io.mango.payment.core.service.MangoPayVirtualPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment/mango-pay/virtual")
@RequiredArgsConstructor
@Tag(name = "芒果支付", description = "芒果支付内置虚拟通道接口")
public class MangoPayVirtualPaymentController implements MangoPayVirtualPaymentApi {

    private final MangoPayVirtualPaymentService virtualPaymentService;

    @Override
    @PostMapping("/pay")
    @Operation(summary = "提交芒果支付")
    public R<MangoPayVirtualPaymentResultVO> pay(@RequestBody MangoPayVirtualPaymentCommand command) {
        return R.ok(virtualPaymentService.pay(command));
    }
}
