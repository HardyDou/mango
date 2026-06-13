package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentSettlementSummaryApi;
import io.mango.payment.api.command.ConfirmPaymentSettlementSummaryCommand;
import io.mango.payment.api.command.GeneratePaymentSettlementSummaryCommand;
import io.mango.payment.api.command.VoidPaymentSettlementSummaryCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentSettlementSummaryStatusVO;
import io.mango.payment.api.vo.PaymentSettlementSummaryVO;
import io.mango.payment.core.service.PaymentSettlementSummaryService;
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
@RequestMapping("/payment/settlement-summaries")
@RequiredArgsConstructor
@Tag(name = "结算汇总", description = "支付域财务结算汇总接口")
public class PaymentSettlementSummaryController implements PaymentSettlementSummaryApi {

    private final PaymentSettlementSummaryService settlementSummaryService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:settlement-summary:list")
    @Operation(summary = "分页查询结算汇总", description = "按日、应用、企业主体和通道查询支付、退款、手续费、净收款汇总")
    public R<PageResult<PaymentSettlementSummaryVO>> pageSettlementSummaries(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(settlementSummaryService.pageSettlementSummaries(query));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:settlement-summary:query")
    @Operation(summary = "查询结算汇总详情", description = "按结算汇总 ID 查询生成、确认、作废和汇总指标")
    public R<PaymentSettlementSummaryVO> detailSettlementSummary(@Parameter(description = "结算汇总 ID", required = true) @RequestParam Long id) {
        return R.ok(settlementSummaryService.detailSettlementSummary(id));
    }

    @Override
    @GetMapping("/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:settlement-summary:list")
    @Operation(summary = "查询结算汇总状态选项", description = "返回结算汇总后台筛选使用的状态契约")
    public R<List<PaymentSettlementSummaryStatusVO>> listSettlementSummaryStatuses() {
        return R.ok(settlementSummaryService.listSettlementSummaryStatuses());
    }

    @Override
    @PostMapping("/generate")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:settlement-summary:generate")
    @Operation(summary = "生成结算汇总", description = "按日期、应用、企业主体和通道生成财务核对汇总，不触发自动付款或会计凭证")
    public R<PaymentSettlementSummaryVO> generateSettlementSummary(@Valid @RequestBody GeneratePaymentSettlementSummaryCommand command) {
        return R.ok(settlementSummaryService.generateSettlementSummary(command));
    }

    @Override
    @PostMapping("/confirm")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:settlement-summary:confirm")
    @Operation(summary = "确认结算汇总", description = "确认前校验对应范围已对账且不存在未处理差异")
    public R<PaymentSettlementSummaryVO> confirmSettlementSummary(@Valid @RequestBody ConfirmPaymentSettlementSummaryCommand command) {
        return R.ok(settlementSummaryService.confirmSettlementSummary(command));
    }

    @Override
    @PostMapping("/void")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:settlement-summary:void")
    @Operation(summary = "作废结算汇总", description = "已确认汇总不可覆盖，修正需先作废再重新生成")
    public R<PaymentSettlementSummaryVO> voidSettlementSummary(@Valid @RequestBody VoidPaymentSettlementSummaryCommand command) {
        return R.ok(settlementSummaryService.voidSettlementSummary(command));
    }
}
