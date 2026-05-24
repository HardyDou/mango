package io.mango.payment.starter.controller;

import io.mango.common.result.R;
import io.mango.payment.api.PaymentApi;
import io.mango.payment.api.command.ClosePayBizOrderCommand;
import io.mango.payment.api.command.CreatePayBizOrderCommand;
import io.mango.payment.api.command.PayCommand;
import io.mango.payment.api.command.PaymentNotifyCommand;
import io.mango.payment.api.command.QueryPayBizOrderCommand;
import io.mango.payment.api.command.QueryPaymentOrderCommand;
import io.mango.payment.api.command.RefreshPaymentStatusCommand;
import io.mango.payment.api.vo.PayBizOrderVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.core.service.IPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Tag(name = "支付中心", description = "业务支付单、支付单、支付状态刷新和支付回调接口")
public class PaymentController implements PaymentApi {

    private final IPaymentService paymentService;

    @Override
    @PostMapping("/biz-orders")
    @Operation(summary = "创建业务支付单", description = "按业务方订单号创建支付中心业务单，重复创建返回原业务单 ID")
    public R<Long> createBizOrder(@Valid @RequestBody CreatePayBizOrderCommand command) {
        return R.ok(paymentService.createBizOrder(command));
    }

    @Override
    @PostMapping("/payments")
    @Operation(summary = "发起支付", description = "基于业务支付单发起支付并返回支付材料")
    public R<PaymentOrderVO> pay(@Valid @RequestBody PayCommand command) {
        return R.ok(paymentService.pay(command));
    }

    @Override
    @PostMapping("/biz-orders/close")
    @Operation(summary = "关闭业务支付单", description = "关闭未支付的业务支付单并关闭处理中支付单")
    public R<Boolean> closeBizOrder(@Valid @RequestBody ClosePayBizOrderCommand command) {
        return R.ok(paymentService.closeBizOrder(command));
    }

    @Override
    @PostMapping("/biz-orders/query")
    @Operation(summary = "查询业务支付单", description = "查询业务支付单聚合状态")
    public R<PayBizOrderVO> queryBizOrder(@Valid @RequestBody QueryPayBizOrderCommand command) {
        return R.ok(paymentService.queryBizOrder(command));
    }

    @Override
    @PostMapping("/payments/query")
    @Operation(summary = "查询支付单", description = "查询支付单状态和支付材料")
    public R<PaymentOrderVO> queryPaymentOrder(@Valid @RequestBody QueryPaymentOrderCommand command) {
        return R.ok(paymentService.queryPaymentOrder(command));
    }

    @Override
    @PostMapping("/payments/refresh")
    @Operation(summary = "刷新支付状态", description = "主动查询渠道并刷新支付单状态")
    public R<PaymentOrderVO> refreshPaymentStatus(@Valid @RequestBody RefreshPaymentStatusCommand command) {
        return R.ok(paymentService.refreshPaymentStatus(command));
    }

    @Override
    @PostMapping("/payments/notify")
    @Operation(summary = "处理支付回调", description = "校验渠道回调签名并幂等推进支付状态")
    public R<Boolean> paymentNotify(@Valid @RequestBody PaymentNotifyCommand command) {
        return R.ok(paymentService.paymentNotify(command));
    }
}
