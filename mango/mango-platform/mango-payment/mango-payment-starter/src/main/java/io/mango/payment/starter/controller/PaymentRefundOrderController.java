package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentRefundOrderApi;
import io.mango.payment.api.command.QueryPaymentRefundOrderCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentRefundOrderStatusVO;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.core.service.PaymentRefundOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/payment/refund-orders")
@RequiredArgsConstructor
@Tag(name = "退款订单", description = "退款申请、退款状态和通道退款结果接口")
public class PaymentRefundOrderController implements PaymentRefundOrderApi {

    private final PaymentRefundOrderService refundOrderService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:refund-order:list")
    @Operation(summary = "分页查询退款订单", description = "查询退款申请、退款状态、原支付订单和通道退款结果")
    public R<PageResult<PaymentRefundOrderVO>> pageRefundOrders(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(refundOrderService.pageRefundOrders(query));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:refund-order:query")
    @Operation(summary = "查询退款订单详情", description = "按退款订单 ID 查询退款申请、通道结果和状态流转")
    public R<PaymentRefundOrderVO> detailRefundOrder(@Parameter(description = "退款订单 ID", required = true) @RequestParam Long id) {
        return R.ok(refundOrderService.detailRefundOrder(id));
    }

    @Override
    @GetMapping("/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:refund-order:list")
    @Operation(summary = "查询退款订单状态选项", description = "返回退款订单后台筛选使用的状态契约")
    public R<List<PaymentRefundOrderStatusVO>> listRefundOrderStatuses() {
        return R.ok(refundOrderService.listRefundOrderStatuses());
    }

    @Override
    @PostMapping("/query-channel")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:refund-order:query-channel")
    @Operation(summary = "主动查询退款订单", description = "对退款中的订单执行通道查询，并按可信查询结果推进退款订单状态")
    public R<PaymentRefundOrderVO> queryRefundOrder(@Valid @RequestBody QueryPaymentRefundOrderCommand command) {
        return R.ok(refundOrderService.queryRefundOrder(command));
    }
}
