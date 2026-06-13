package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentBusinessOrderApi;
import io.mango.payment.api.command.CreatePaymentBusinessOrderCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentBusinessOrderStatusVO;
import io.mango.payment.api.vo.PaymentBusinessOrderVO;
import io.mango.payment.core.service.PaymentBusinessOrderService;
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
@RequestMapping("/payment/business-orders")
@RequiredArgsConstructor
@Tag(name = "支付业务订单", description = "业务系统提交到支付平台的支付意图和状态接口")
public class PaymentBusinessOrderController implements PaymentBusinessOrderApi {

    private final PaymentBusinessOrderService businessOrderService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:business-order:list")
    @Operation(summary = "分页查询业务订单", description = "查询业务系统提交到支付平台的支付意图和状态")
    public R<PageResult<PaymentBusinessOrderVO>> pageBusinessOrders(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(businessOrderService.pageBusinessOrders(query));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:business-order:query")
    @Operation(summary = "查询业务订单详情", description = "按业务订单 ID 查询支付意图详情")
    public R<PaymentBusinessOrderVO> detailBusinessOrder(@Parameter(description = "业务订单 ID", required = true) @RequestParam Long id) {
        return R.ok(businessOrderService.detailBusinessOrder(id));
    }

    @Override
    @GetMapping("/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:business-order:list")
    @Operation(summary = "查询业务订单状态选项", description = "返回业务订单后台筛选使用的状态契约")
    public R<List<PaymentBusinessOrderStatusVO>> listBusinessOrderStatuses() {
        return R.ok(businessOrderService.listBusinessOrderStatuses());
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:business-order:create")
    @Operation(summary = "创建业务订单", description = "后台创建待支付业务订单，初始状态为待支付，金额按分保存")
    public R<PaymentBusinessOrderVO> createBusinessOrder(@Valid @RequestBody CreatePaymentBusinessOrderCommand command) {
        return R.ok(businessOrderService.createBusinessOrder(command));
    }
}
