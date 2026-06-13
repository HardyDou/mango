package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentTransactionFlowApi;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentTransactionFlowVO;
import io.mango.payment.core.service.PaymentTransactionFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/payment/transaction-flows")
@RequiredArgsConstructor
@Tag(name = "交易流水", description = "支付域资金事件流水接口")
public class PaymentTransactionFlowController implements PaymentTransactionFlowApi {

    private final PaymentTransactionFlowService transactionFlowService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:transaction-flow:list")
    @Operation(summary = "分页查询交易流水", description = "查询支付成功、退款成功、手续费等支付域资金事件")
    public R<PageResult<PaymentTransactionFlowVO>> pageTransactionFlows(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(transactionFlowService.pageTransactionFlows(query));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:transaction-flow:query")
    @Operation(summary = "查询交易流水详情", description = "按交易流水 ID 查询支付域资金事件明细和关联订单")
    public R<PaymentTransactionFlowVO> detailTransactionFlow(@Parameter(description = "交易流水 ID", required = true) @RequestParam Long id) {
        return R.ok(transactionFlowService.detailTransactionFlow(id));
    }
}
