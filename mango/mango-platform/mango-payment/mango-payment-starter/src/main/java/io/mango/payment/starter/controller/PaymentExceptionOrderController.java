package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentExceptionOrderApi;
import io.mango.payment.api.command.HandlePaymentExceptionOrderCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentExceptionOrderActionVO;
import io.mango.payment.api.vo.PaymentExceptionOrderStatusVO;
import io.mango.payment.api.vo.PaymentExceptionOrderVO;
import io.mango.payment.core.service.PaymentExceptionOrderService;
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
@RequestMapping("/payment/exception-orders")
@RequiredArgsConstructor
@Tag(name = "异常订单", description = "支付异常订单查询和受控处理接口")
public class PaymentExceptionOrderController implements PaymentExceptionOrderApi {

    private final PaymentExceptionOrderService exceptionOrderService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:exception-order:list")
    @Operation(summary = "分页查询异常订单", description = "查询重复支付、超时未回调、金额不一致、状态不一致等异常订单")
    public R<PageResult<PaymentExceptionOrderVO>> pageExceptionOrders(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(exceptionOrderService.pageExceptionOrders(query));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:exception-order:query")
    @Operation(summary = "查询异常订单详情", description = "按异常订单 ID 查询异常原因、处理记录和凭据")
    public R<PaymentExceptionOrderVO> detailExceptionOrder(@Parameter(description = "异常订单 ID", required = true) @RequestParam Long id) {
        return R.ok(exceptionOrderService.detailExceptionOrder(id));
    }

    @Override
    @GetMapping("/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:exception-order:list")
    @Operation(summary = "查询异常订单处理状态选项", description = "返回异常订单后台筛选使用的处理状态契约")
    public R<List<PaymentExceptionOrderStatusVO>> listExceptionOrderStatuses() {
        return R.ok(exceptionOrderService.listExceptionOrderStatuses());
    }

    @Override
    @GetMapping("/actions")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:exception-order:handle")
    @Operation(summary = "查询异常订单处理动作选项", description = "返回异常订单后台受控处理动作契约")
    public R<List<PaymentExceptionOrderActionVO>> listExceptionOrderActions() {
        return R.ok(exceptionOrderService.listExceptionOrderActions());
    }

    @Override
    @PostMapping("/handle")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:exception-order:handle")
    @Operation(summary = "处理异常订单", description = "受控处理异常订单，仅记录异常处理闭环，不直接修改支付或退款订单成功状态")
    public R<PaymentExceptionOrderVO> handleExceptionOrder(@Valid @RequestBody HandlePaymentExceptionOrderCommand command) {
        return R.ok(exceptionOrderService.handleExceptionOrder(command));
    }
}
