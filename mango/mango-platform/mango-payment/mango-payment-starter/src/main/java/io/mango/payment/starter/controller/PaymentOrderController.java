package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentOrderApi;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentOrderStatusVO;
import io.mango.payment.api.vo.PaymentOrderSyncStatusVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.core.service.PaymentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/payment/payment-orders")
@RequiredArgsConstructor
@Tag(name = "支付订单", description = "支付尝试、通道请求和支付状态接口")
public class PaymentOrderController implements PaymentOrderApi {

    private final PaymentOrderService paymentOrderService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:payment-order:list")
    @Operation(summary = "分页查询支付订单", description = "查询支付尝试、支付状态、通道请求和实际通道信息")
    public R<PageResult<PaymentOrderVO>> pagePaymentOrders(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(paymentOrderService.pagePaymentOrders(query));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:payment-order:query")
    @Operation(summary = "查询支付订单详情", description = "按支付订单 ID 查询通道请求、状态流转和关联业务订单信息")
    public R<PaymentOrderVO> detailPaymentOrder(@Parameter(description = "支付订单 ID", required = true) @RequestParam Long id) {
        return R.ok(paymentOrderService.detailPaymentOrder(id));
    }

    @Override
    @GetMapping("/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:payment-order:list")
    @Operation(summary = "查询支付订单状态选项", description = "返回支付订单后台筛选使用的状态契约")
    public R<List<PaymentOrderStatusVO>> listPaymentOrderStatuses() {
        return R.ok(paymentOrderService.listPaymentOrderStatuses());
    }

    @Override
    @PostMapping("/sync-status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:payment-order:sync-status")
    @Operation(summary = "同步支付订单状态", description = "按支付订单号调用支付通道查单，并通过统一支付状态机推进后续流水、业务订单和通知流程")
    public R<PaymentOrderSyncStatusVO> syncPaymentOrderStatus(
            @Parameter(description = "支付订单号", required = true) @NotBlank(message = "支付订单号不能为空") @RequestParam String payOrderNo) {
        return R.ok(paymentOrderService.syncPaymentOrderStatus(payOrderNo));
    }
}
