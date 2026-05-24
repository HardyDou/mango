package io.mango.payment.starter.controller;

import io.mango.common.result.R;
import io.mango.payment.api.SandboxPaymentApi;
import io.mango.payment.api.command.SandboxPaymentCommand;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.SandboxPaymentNotifyVO;
import io.mango.payment.starter.service.ISandboxPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment/sandbox")
@RequiredArgsConstructor
@Tag(name = "支付沙箱", description = "独立沙箱通道支付和回调报文生成接口")
public class SandboxPaymentController implements SandboxPaymentApi {

    private final ISandboxPaymentService sandboxPaymentService;

    @Override
    @PostMapping("/payment-notifies")
    @Operation(summary = "生成沙箱支付回调", description = "按沙箱签名协议生成可提交给支付回调接口的回调报文")
    public R<SandboxPaymentNotifyVO> createPaymentNotify(@Valid @RequestBody SandboxPaymentCommand command) {
        return R.ok(sandboxPaymentService.createPaymentNotify(command));
    }

    @Override
    @PostMapping("/payments/complete")
    @Operation(summary = "完成沙箱付款", description = "按沙箱通道协议完成付款，并复用正式回调链路推进支付状态")
    public R<PaymentOrderVO> completePayment(@Valid @RequestBody SandboxPaymentCommand command) {
        return R.ok(sandboxPaymentService.completePayment(command));
    }
}
