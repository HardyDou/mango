package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentReconciliationApi;
import io.mango.payment.api.command.FetchPaymentChannelBillCommand;
import io.mango.payment.api.command.GenerateMangoPayVirtualBillCommand;
import io.mango.payment.api.command.GeneratePaymentChannelBillCommand;
import io.mango.payment.api.command.ImportPaymentReconciliationCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentChannelBillFetchBatchVO;
import io.mango.payment.api.vo.PaymentChannelBillFetchModeVO;
import io.mango.payment.api.vo.PaymentChannelBillSourceVO;
import io.mango.payment.api.vo.PaymentReconciliationStatusVO;
import io.mango.payment.api.vo.PaymentReconciliationVO;
import io.mango.payment.core.service.PaymentReconciliationService;
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
@RequestMapping("/payment/reconciliations")
@RequiredArgsConstructor
@Tag(name = "支付对账", description = "通道账单、对账批次和自动账单获取接口")
public class PaymentReconciliationController implements PaymentReconciliationApi {

    private final PaymentReconciliationService reconciliationService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:reconciliation:list")
    @Operation(summary = "分页查询对账批次", description = "查询通道账单导入批次、文件摘要、导入人和对账结果")
    public R<PageResult<PaymentReconciliationVO>> pageReconciliations(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(reconciliationService.pageReconciliations(query));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:reconciliation:query")
    @Operation(summary = "查询对账批次详情", description = "按对账批次 ID 查询账单导入记录和通道账单明细")
    public R<PaymentReconciliationVO> detailReconciliation(@Parameter(description = "对账批次 ID", required = true) @RequestParam Long id) {
        return R.ok(reconciliationService.detailReconciliation(id));
    }

    @Override
    @GetMapping("/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:reconciliation:list")
    @Operation(summary = "查询对账状态选项", description = "返回对账管理后台筛选使用的状态契约")
    public R<List<PaymentReconciliationStatusVO>> listReconciliationStatuses() {
        return R.ok(reconciliationService.listReconciliationStatuses());
    }

    @Override
    @GetMapping("/bill-fetch-modes")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:reconciliation:list")
    @Operation(summary = "查询通道账单获取方式选项", description = "返回手动、FTP、FTPS、HTTP 等通道账单获取方式契约")
    public R<List<PaymentChannelBillFetchModeVO>> listBillFetchModes() {
        return R.ok(reconciliationService.listBillFetchModes());
    }

    @Override
    @PostMapping("/import")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:reconciliation:import")
    @Operation(summary = "导入通道账单", description = "导入通道账单形成对账批次，同一通道、日期、文件摘要只能导入一次，并执行支付成功金额核对")
    public R<PaymentReconciliationVO> importReconciliation(@Valid @RequestBody ImportPaymentReconciliationCommand command) {
        return R.ok(reconciliationService.importReconciliation(command));
    }

    @Override
    @PostMapping("/mango-pay/virtual/generate")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:reconciliation:import")
    @Operation(summary = "生成芒果支付账单", description = "按账单日期从芒果支付真实支付和退款订单生成通道账单批次，并执行对账核对")
    public R<PaymentReconciliationVO> generateMangoPayVirtualBill(@Valid @RequestBody GenerateMangoPayVirtualBillCommand command) {
        return R.ok(reconciliationService.generateMangoPayVirtualBill(command));
    }

    @Override
    @PostMapping("/channel-bill/generate")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:reconciliation:import")
    @Operation(summary = "生成通道账单", description = "按通道适配器真实账单能力生成通道账单批次，并进入统一对账流程")
    public R<PaymentReconciliationVO> generatePaymentChannelBill(@Valid @RequestBody GeneratePaymentChannelBillCommand command) {
        return R.ok(reconciliationService.generatePaymentChannelBill(command));
    }

    @Override
    @GetMapping("/bill-sources/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:reconciliation:list")
    @Operation(summary = "分页查询通道账单获取源", description = "查询手动、FTP、FTPS、HTTP 等通道账单获取方式配置")
    public R<PageResult<PaymentChannelBillSourceVO>> pageBillSources(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(reconciliationService.pageBillSources(query));
    }

    @Override
    @GetMapping("/bill-sources/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:reconciliation:query")
    @Operation(summary = "查询通道账单获取源详情", description = "按配置 ID 查询通道账单获取源")
    public R<PaymentChannelBillSourceVO> detailBillSource(@Parameter(description = "账单获取源 ID", required = true) @RequestParam Long id) {
        return R.ok(reconciliationService.detailBillSource(id));
    }

    @Override
    @GetMapping("/bill-fetch-batches/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:reconciliation:list")
    @Operation(summary = "分页查询通道账单获取批次", description = "查询通道账单自动获取执行记录、响应摘要和关联对账批次")
    public R<PageResult<PaymentChannelBillFetchBatchVO>> pageBillFetchBatches(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(reconciliationService.pageBillFetchBatches(query));
    }

    @Override
    @PostMapping("/bill-fetch")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:reconciliation:import")
    @Operation(summary = "发起通道账单获取", description = "按通道账单获取源拉取原始账单并进入统一对账导入流程")
    public R<PaymentReconciliationVO> fetchChannelBill(@Valid @RequestBody FetchPaymentChannelBillCommand command) {
        return R.ok(reconciliationService.fetchChannelBill(command));
    }
}
