package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.payment.api.PaymentTaskApi;
import io.mango.payment.api.vo.PaymentTaskDispatchResultVO;
import io.mango.payment.core.service.PaymentTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/payment/tasks")
@RequiredArgsConstructor
@Tag(name = "支付任务", description = "支付域人工触发任务接口")
public class PaymentTaskController implements PaymentTaskApi {

    private final PaymentTaskService taskService;

    @Override
    @PostMapping("/expire-open-orders")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:task:expire-open-orders")
    @Operation(summary = "关闭已过期待支付订单", description = "人工或平台调度触发当前租户已过期且未成功支付的订单关闭任务，按受控关单链路推进并记录审计")
    public R<PaymentTaskDispatchResultVO> expireOpenPaymentOrders(
            @Parameter(description = "本次最多扫描订单数，1-100") @RequestParam(defaultValue = "20") long limit) {
        return R.ok(taskService.expireOpenPaymentOrders(limit));
    }

    @Override
    @PostMapping("/query-processing-orders")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:task:query-processing-orders")
    @Operation(summary = "批量主动查询支付中订单", description = "人工或平台调度触发当前租户支付中订单主动查单任务，逐笔调用通道查单并记录批次审计")
    public R<PaymentTaskDispatchResultVO> queryProcessingPaymentOrders(
            @Parameter(description = "本次最多扫描订单数，1-100") @RequestParam(defaultValue = "20") long limit) {
        return R.ok(taskService.queryProcessingPaymentOrders(limit));
    }
}
