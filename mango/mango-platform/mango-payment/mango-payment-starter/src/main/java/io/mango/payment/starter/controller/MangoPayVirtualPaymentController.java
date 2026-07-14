package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.payment.api.MangoPayVirtualPaymentApi;
import io.mango.payment.api.command.CreateMangoPayScenarioControlCommand;
import io.mango.payment.api.command.MangoPayVirtualPaymentCommand;
import io.mango.payment.api.vo.MangoPayVirtualPaymentResultVO;
import io.mango.payment.core.service.IPaymentMangoPayScenarioControlService;
import io.mango.payment.core.service.IMangoPayVirtualPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/payment/mango-pay/virtual")
@RequiredArgsConstructor
@Tag(name = "芒果支付", description = "芒果支付内置虚拟通道接口")
public class MangoPayVirtualPaymentController implements MangoPayVirtualPaymentApi {

    private final IMangoPayVirtualPaymentService virtualPaymentService;
    private final IPaymentMangoPayScenarioControlService scenarioControlService;

    @Override
    @PostMapping("/pay")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "芒果支付内置通道收银台支付")
    @Operation(summary = "提交芒果支付", description = "通过芒果支付虚拟通道执行收银台支付")
    public R<MangoPayVirtualPaymentResultVO> pay(@Valid @RequestBody MangoPayVirtualPaymentCommand command) {
        return R.ok(virtualPaymentService.pay(command));
    }

    @Override
    @PostMapping("/scenario-controls")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:mango-pay:scenario-control")
    @Operation(summary = "创建芒果支付异常场景控制", description = "控制 MANGO_PAY 通道下一笔支付、查单、退款、退款查询或账单差异场景")
    public R<Long> createMangoPayScenarioControl(@Valid @RequestBody CreateMangoPayScenarioControlCommand command) {
        return R.ok(scenarioControlService.createScenarioControl(command));
    }
}
