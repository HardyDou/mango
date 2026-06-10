package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.ConfirmPaymentSettlementSummaryCommand;
import io.mango.payment.api.command.ConfirmOfflineBankStatementMatchCommand;
import io.mango.payment.api.command.ConfirmOfflineCollectionCommand;
import io.mango.payment.api.command.CreateOfflineRefundCommand;
import io.mango.payment.api.command.CreateMangoPayScenarioControlCommand;
import io.mango.payment.api.command.CreatePaymentBusinessOrderCommand;
import io.mango.payment.api.command.CreatePaymentRefundApprovalCommand;
import io.mango.payment.api.command.GenerateMangoPayVirtualBillCommand;
import io.mango.payment.api.command.GeneratePaymentSettlementSummaryCommand;
import io.mango.payment.api.command.HandlePaymentDifferenceCommand;
import io.mango.payment.api.command.HandlePaymentExceptionOrderCommand;
import io.mango.payment.api.command.ImportPaymentReconciliationCommand;
import io.mango.payment.api.command.QueryPaymentRefundOrderCommand;
import io.mango.payment.api.command.ReviewPaymentRefundApprovalCommand;
import io.mango.payment.api.command.RetryPaymentNotificationRecordCommand;
import io.mango.payment.api.command.VoidPaymentSettlementSummaryCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentBusinessOrderStatusVO;
import io.mango.payment.api.vo.PaymentBusinessOrderVO;
import io.mango.payment.api.vo.PaymentChannelCapabilityVO;
import io.mango.payment.api.vo.PaymentDifferenceActionVO;
import io.mango.payment.api.vo.PaymentDifferenceStatusVO;
import io.mango.payment.api.vo.PaymentDifferenceVO;
import io.mango.payment.api.vo.PaymentExceptionOrderActionVO;
import io.mango.payment.api.vo.PaymentExceptionOrderStatusVO;
import io.mango.payment.api.vo.PaymentExceptionOrderVO;
import io.mango.payment.api.vo.PaymentNotificationRecordVO;
import io.mango.payment.api.vo.PaymentNotificationStatusVO;
import io.mango.payment.api.vo.PaymentObservabilitySnapshotVO;
import io.mango.payment.api.vo.PaymentOfflineBankStatementBatchStatusVO;
import io.mango.payment.api.vo.PaymentOfflineBankStatementBatchVO;
import io.mango.payment.api.vo.PaymentOfflineBankStatementMatchStatusVO;
import io.mango.payment.api.vo.PaymentOfflineCollectionStatusVO;
import io.mango.payment.api.vo.PaymentOfflineCollectionVO;
import io.mango.payment.api.vo.PaymentOfflineRefundStatusVO;
import io.mango.payment.api.vo.PaymentOfflineRefundVO;
import io.mango.payment.api.vo.PaymentOperationAuditVO;
import io.mango.payment.api.vo.PaymentOrderStatusVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.PaymentRefundApprovalStatusVO;
import io.mango.payment.api.vo.PaymentRefundApprovalVO;
import io.mango.payment.api.vo.PaymentRefundOrderStatusVO;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.api.vo.PaymentReconciliationStatusVO;
import io.mango.payment.api.vo.PaymentReconciliationVO;
import io.mango.payment.api.vo.PaymentSettlementSummaryStatusVO;
import io.mango.payment.api.vo.PaymentSettlementSummaryVO;
import io.mango.payment.api.vo.PaymentSensitiveFieldReencryptResultVO;
import io.mango.payment.api.vo.PaymentTaskDispatchResultVO;
import io.mango.payment.api.vo.PaymentTransactionFlowVO;
import io.mango.payment.core.service.PaymentMangoPayScenarioControlService;
import io.mango.payment.core.service.PaymentObservabilityService;
import io.mango.payment.core.service.PaymentOfflineChannelService;
import io.mango.payment.core.service.PaymentReconciliationService;
import io.mango.payment.core.service.PaymentRefundApprovalService;
import io.mango.payment.core.service.PaymentReadonlyResourceService;
import io.mango.payment.core.service.PaymentSettlementSummaryService;
import io.mango.payment.core.service.PaymentSensitiveFieldReencryptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Tag(name = "支付资源查询", description = "支付订单、流水、对账、审计等后台查询接口")
public class PaymentReadonlyResourceController {

    private final PaymentReadonlyResourceService resourceService;
    private final PaymentRefundApprovalService refundApprovalService;
    private final PaymentReconciliationService reconciliationService;
    private final PaymentSettlementSummaryService settlementSummaryService;
    private final PaymentMangoPayScenarioControlService scenarioControlService;
    private final PaymentSensitiveFieldReencryptService sensitiveFieldReencryptService;
    private final PaymentObservabilityService observabilityService;
    private final PaymentOfflineChannelService offlineChannelService;

    @GetMapping("/business-orders/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:business-order:list")
    @Operation(summary = "分页查询业务订单", description = "查询业务系统提交到支付平台的支付意图和状态")
    public R<PageResult<PaymentBusinessOrderVO>> pageBusinessOrders(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(resourceService.pageBusinessOrders(query));
    }

    @GetMapping("/business-orders/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:business-order:query")
    @Operation(summary = "查询业务订单详情", description = "按业务订单 ID 查询支付意图详情")
    public R<PaymentBusinessOrderVO> detailBusinessOrder(@Parameter(description = "业务订单 ID", required = true) @RequestParam Long id) {
        return R.ok(resourceService.detailBusinessOrder(id));
    }

    @GetMapping("/business-orders/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:business-order:list")
    @Operation(summary = "查询业务订单状态选项", description = "返回业务订单后台筛选使用的状态契约")
    public R<List<PaymentBusinessOrderStatusVO>> listBusinessOrderStatuses() {
        return R.ok(resourceService.listBusinessOrderStatuses());
    }

    @PostMapping("/business-orders")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:business-order:create")
    @Operation(summary = "创建业务订单", description = "后台创建待支付业务订单，初始状态为待支付，金额按分保存")
    public R<PaymentBusinessOrderVO> createBusinessOrder(@Valid @RequestBody CreatePaymentBusinessOrderCommand command) {
        return R.ok(resourceService.createBusinessOrder(command));
    }

    @GetMapping("/payment-orders/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:payment-order:list")
    @Operation(summary = "分页查询支付订单", description = "查询支付尝试、支付状态、通道请求和实际通道信息")
    public R<PageResult<PaymentOrderVO>> pagePaymentOrders(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(resourceService.pagePaymentOrders(query));
    }

    @GetMapping("/payment-orders/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:payment-order:query")
    @Operation(summary = "查询支付订单详情", description = "按支付订单 ID 查询通道请求、状态流转和关联业务订单信息")
    public R<PaymentOrderVO> detailPaymentOrder(@Parameter(description = "支付订单 ID", required = true) @RequestParam Long id) {
        return R.ok(resourceService.detailPaymentOrder(id));
    }

    @GetMapping("/payment-orders/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:payment-order:list")
    @Operation(summary = "查询支付订单状态选项", description = "返回支付订单后台筛选使用的状态契约")
    public R<List<PaymentOrderStatusVO>> listPaymentOrderStatuses() {
        return R.ok(resourceService.listPaymentOrderStatuses());
    }

    @GetMapping("/offline-collections/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:list")
    @Operation(summary = "分页查询线下收款", description = "查询线下转账收款单、随机对账码、转账备注、凭证数量和到账状态")
    public R<PageResult<PaymentOfflineCollectionVO>> pageOfflineCollections(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(resourceService.pageOfflineCollections(query));
    }

    @GetMapping("/offline-collections/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:query")
    @Operation(summary = "查询线下收款详情", description = "按线下收款 ID 查询收款账户、对账码、关联支付订单和到账状态")
    public R<PaymentOfflineCollectionVO> detailOfflineCollection(@Parameter(description = "线下收款 ID", required = true) @RequestParam Long id) {
        return R.ok(resourceService.detailOfflineCollection(id));
    }

    @GetMapping("/offline-collections/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:list")
    @Operation(summary = "查询线下收款状态选项", description = "返回线下收款后台筛选使用的状态契约")
    public R<List<PaymentOfflineCollectionStatusVO>> listOfflineCollectionStatuses() {
        return R.ok(resourceService.listOfflineCollectionStatuses());
    }

    @PostMapping("/offline-collections/confirm")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:confirm")
    @Operation(summary = "确认线下收款到账", description = "财务确认线下转账到账后推进线下收款、支付订单和业务订单状态")
    public R<PaymentOfflineCollectionVO> confirmOfflineCollection(@Valid @RequestBody ConfirmOfflineCollectionCommand command) {
        return R.ok(offlineChannelService.confirmCollection(command));
    }

    @GetMapping("/offline-collections/bank-statements/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:bank-statement:list")
    @Operation(summary = "分页查询线下银行流水导入批次", description = "查询线下收款通道银行流水 Excel 导入批次、匹配和确认结果")
    public R<PageResult<PaymentOfflineBankStatementBatchVO>> pageOfflineBankStatements(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(offlineChannelService.pageBankStatementBatches(query));
    }

    @GetMapping("/offline-collections/bank-statements/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:bank-statement:query")
    @Operation(summary = "查询线下银行流水批次详情", description = "按批次 ID 查询银行流水明细、匹配状态和确认结果")
    public R<PaymentOfflineBankStatementBatchVO> detailOfflineBankStatement(@Parameter(description = "银行流水批次 ID", required = true) @RequestParam Long id) {
        return R.ok(offlineChannelService.detailBankStatementBatch(id));
    }

    @GetMapping("/offline-collections/bank-statements/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:bank-statement:list")
    @Operation(summary = "查询线下银行流水批次状态选项", description = "返回线下银行流水批次后台筛选使用的状态契约")
    public R<List<PaymentOfflineBankStatementBatchStatusVO>> listOfflineBankStatementStatuses() {
        return R.ok(offlineChannelService.listBankStatementBatchStatuses());
    }

    @GetMapping("/offline-collections/bank-statements/match-statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:bank-statement:list")
    @Operation(summary = "查询线下银行流水匹配状态选项", description = "返回线下银行流水明细匹配状态契约")
    public R<List<PaymentOfflineBankStatementMatchStatusVO>> listOfflineBankStatementMatchStatuses() {
        return R.ok(offlineChannelService.listBankStatementMatchStatuses());
    }

    @PostMapping("/offline-collections/bank-statements/import")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:bank-statement:import")
    @Operation(summary = "导入线下银行流水 Excel", description = "后端解析银行流水 Excel，落批次和明细，并按对账码、金额和状态生成匹配结果")
    public R<PaymentOfflineBankStatementBatchVO> importOfflineBankStatement(
            @Parameter(description = "银行流水 Excel 文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "文件中心 ID，可为空") @RequestParam(required = false) Long statementFileId) throws IOException {
        return R.ok(offlineChannelService.importBankStatement(file.getBytes(), file.getOriginalFilename(), statementFileId));
    }

    @PostMapping("/offline-collections/bank-statements/confirm")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:bank-statement:confirm")
    @Operation(summary = "确认线下银行流水匹配到账", description = "财务确认匹配银行流水后推进线下收款、支付订单和业务订单状态")
    public R<PaymentOfflineBankStatementBatchVO> confirmOfflineBankStatementMatch(@Valid @RequestBody ConfirmOfflineBankStatementMatchCommand command) {
        return R.ok(offlineChannelService.confirmBankStatementMatches(command));
    }

    @PostMapping("/offline-collections/refund")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:refund")
    @Operation(summary = "创建线下退款", description = "线下收款通道录入退款金额、退款账户和退款凭证，支持部分退款")
    public R<PaymentOfflineRefundVO> createOfflineRefund(@Valid @RequestBody CreateOfflineRefundCommand command) {
        return R.ok(offlineChannelService.createOfflineRefund(command));
    }

    @GetMapping("/offline-refunds/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-refund:list")
    @Operation(summary = "分页查询线下退款订单", description = "查询线下收款通道独立退款订单、退款账户、退款凭证和退款状态")
    public R<PageResult<PaymentOfflineRefundVO>> pageOfflineRefunds(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(offlineChannelService.pageOfflineRefunds(query));
    }

    @GetMapping("/offline-refunds/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-refund:query")
    @Operation(summary = "查询线下退款详情", description = "按线下退款 ID 查询退款金额、账户、凭证和关联线下收款单")
    public R<PaymentOfflineRefundVO> detailOfflineRefund(@Parameter(description = "线下退款 ID", required = true) @RequestParam Long id) {
        return R.ok(offlineChannelService.detailOfflineRefund(id));
    }

    @GetMapping("/offline-refunds/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-refund:list")
    @Operation(summary = "查询线下退款状态选项", description = "返回线下退款后台筛选使用的状态契约")
    public R<List<PaymentOfflineRefundStatusVO>> listOfflineRefundStatuses() {
        return R.ok(offlineChannelService.listOfflineRefundStatuses());
    }

    @GetMapping("/refund-orders/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:refund-order:list")
    @Operation(summary = "分页查询退款订单", description = "查询退款申请、退款状态、原支付订单和通道退款结果")
    public R<PageResult<PaymentRefundOrderVO>> pageRefundOrders(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(resourceService.pageRefundOrders(query));
    }

    @GetMapping("/refund-orders/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:refund-order:query")
    @Operation(summary = "查询退款订单详情", description = "按退款订单 ID 查询退款申请、通道结果和状态流转")
    public R<PaymentRefundOrderVO> detailRefundOrder(@Parameter(description = "退款订单 ID", required = true) @RequestParam Long id) {
        return R.ok(resourceService.detailRefundOrder(id));
    }

    @GetMapping("/refund-orders/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:refund-order:list")
    @Operation(summary = "查询退款订单状态选项", description = "返回退款订单后台筛选使用的状态契约")
    public R<List<PaymentRefundOrderStatusVO>> listRefundOrderStatuses() {
        return R.ok(resourceService.listRefundOrderStatuses());
    }

    @PostMapping("/refund-orders/query-channel")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:refund-order:query-channel")
    @Operation(summary = "主动查询退款订单", description = "对退款中的芒果支付退款订单执行通道查询，并按可信查询结果推进退款订单状态")
    public R<PaymentRefundOrderVO> queryRefundOrder(@Valid @RequestBody QueryPaymentRefundOrderCommand command) {
        return R.ok(resourceService.queryRefundOrder(command));
    }

    @GetMapping("/refund-approvals/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:refund-approval:list")
    @Operation(summary = "分页查询退款审批", description = "查询后台发起退款的审批单、审批状态和关联退款订单")
    public R<PageResult<PaymentRefundApprovalVO>> pageRefundApprovals(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(refundApprovalService.pageRefundApprovals(query));
    }

    @GetMapping("/refund-approvals/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:refund-approval:query")
    @Operation(summary = "查询退款审批详情", description = "按退款审批 ID 查询审批单、申请人、审核人和关联退款结果")
    public R<PaymentRefundApprovalVO> detailRefundApproval(@Parameter(description = "退款审批 ID", required = true) @RequestParam Long id) {
        return R.ok(refundApprovalService.detailRefundApproval(id));
    }

    @GetMapping("/refund-approvals/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:refund-approval:list")
    @Operation(summary = "查询退款审批状态选项", description = "返回退款审批后台筛选使用的状态契约")
    public R<List<PaymentRefundApprovalStatusVO>> listRefundApprovalStatuses() {
        return R.ok(refundApprovalService.listRefundApprovalStatuses());
    }

    @PostMapping("/refund-approvals")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:refund-approval:create")
    @Operation(summary = "创建退款审批", description = "后台发起退款必须先创建审批单，只校验原支付订单、可退金额和幂等号，不直接生成退款成功状态")
    public R<PaymentRefundApprovalVO> createRefundApproval(@Valid @RequestBody CreatePaymentRefundApprovalCommand command) {
        return R.ok(refundApprovalService.createRefundApproval(command));
    }

    @PostMapping("/refund-approvals/review")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:refund-approval:review")
    @Operation(summary = "审核退款审批", description = "审核通过后按既有退款链路发起退款，审核拒绝只关闭审批单；申请人不能审核自己的退款申请")
    public R<PaymentRefundApprovalVO> reviewRefundApproval(@Valid @RequestBody ReviewPaymentRefundApprovalCommand command) {
        return R.ok(refundApprovalService.reviewRefundApproval(command));
    }

    @GetMapping("/transaction-flows/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:transaction-flow:list")
    @Operation(summary = "分页查询交易流水", description = "查询支付成功、退款成功、手续费等支付域资金事件")
    public R<PageResult<PaymentTransactionFlowVO>> pageTransactionFlows(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(resourceService.pageTransactionFlows(query));
    }

    @GetMapping("/transaction-flows/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:transaction-flow:query")
    @Operation(summary = "查询交易流水详情", description = "按交易流水 ID 查询支付域资金事件明细和关联订单")
    public R<PaymentTransactionFlowVO> detailTransactionFlow(@Parameter(description = "交易流水 ID", required = true) @RequestParam Long id) {
        return R.ok(resourceService.detailTransactionFlow(id));
    }

    @GetMapping("/exception-orders/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:exception-order:list")
    @Operation(summary = "分页查询异常订单", description = "查询重复支付、超时未回调、金额不一致、状态不一致等异常订单")
    public R<PageResult<PaymentExceptionOrderVO>> pageExceptionOrders(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(resourceService.pageExceptionOrders(query));
    }

    @GetMapping("/exception-orders/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:exception-order:query")
    @Operation(summary = "查询异常订单详情", description = "按异常订单 ID 查询异常原因、处理记录和凭据")
    public R<PaymentExceptionOrderVO> detailExceptionOrder(@Parameter(description = "异常订单 ID", required = true) @RequestParam Long id) {
        return R.ok(resourceService.detailExceptionOrder(id));
    }

    @GetMapping("/exception-orders/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:exception-order:list")
    @Operation(summary = "查询异常订单处理状态选项", description = "返回异常订单后台筛选使用的处理状态契约")
    public R<List<PaymentExceptionOrderStatusVO>> listExceptionOrderStatuses() {
        return R.ok(resourceService.listExceptionOrderStatuses());
    }

    @GetMapping("/exception-orders/actions")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:exception-order:handle")
    @Operation(summary = "查询异常订单处理动作选项", description = "返回异常订单后台受控处理动作契约")
    public R<List<PaymentExceptionOrderActionVO>> listExceptionOrderActions() {
        return R.ok(resourceService.listExceptionOrderActions());
    }

    @PostMapping("/exception-orders/handle")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:exception-order:handle")
    @Operation(summary = "处理异常订单", description = "受控处理异常订单，仅记录异常处理闭环，不直接修改支付或退款订单成功状态")
    public R<PaymentExceptionOrderVO> handleExceptionOrder(@Valid @RequestBody HandlePaymentExceptionOrderCommand command) {
        return R.ok(resourceService.handleExceptionOrder(command));
    }

    @GetMapping("/notification-records/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:notification-record:list")
    @Operation(summary = "分页查询通知记录", description = "查询支付或退款结果通知业务系统的发送状态、响应和重试信息")
    public R<PageResult<PaymentNotificationRecordVO>> pageNotificationRecords(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(resourceService.pageNotificationRecords(query));
    }

    @GetMapping("/notification-records/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:notification-record:query")
    @Operation(summary = "查询通知记录详情", description = "按通知记录 ID 查询通知目标、响应、重试和人工补偿信息")
    public R<PaymentNotificationRecordVO> detailNotificationRecord(@Parameter(description = "通知记录 ID", required = true) @RequestParam Long id) {
        return R.ok(resourceService.detailNotificationRecord(id));
    }

    @GetMapping("/notification-records/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:notification-record:list")
    @Operation(summary = "查询通知状态选项", description = "返回通知记录后台筛选使用的通知状态契约")
    public R<List<PaymentNotificationStatusVO>> listNotificationStatuses() {
        return R.ok(resourceService.listNotificationStatuses());
    }

    @PostMapping("/notification-records/retry")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:notification-record:retry")
    @Operation(summary = "人工重推通知记录", description = "登记失败通知的人工补偿重推，仅重推已有支付或退款结果，不改变资金状态")
    public R<PaymentNotificationRecordVO> retryNotificationRecord(@Valid @RequestBody RetryPaymentNotificationRecordCommand command) {
        return R.ok(resourceService.retryNotificationRecord(command));
    }

    @PostMapping("/notification-records/deliver-due")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:notification-record:deliver-due")
    @Operation(summary = "投递到期通知记录", description = "人工触发投递当前租户已到计划时间的支付或退款通知记录，不改变资金状态并记录操作审计")
    public R<Integer> deliverDueNotificationRecords(@Parameter(description = "本次最多投递条数，1-100") @RequestParam(defaultValue = "20") long limit) {
        return R.ok(resourceService.deliverDueNotificationRecords(limit));
    }

    @PostMapping("/tasks/expire-open-orders")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:task:expire-open-orders")
    @Operation(summary = "关闭已过期待支付订单", description = "人工或平台调度触发当前租户已过期且未成功支付的订单关闭任务，按受控关单链路推进并记录审计")
    public R<PaymentTaskDispatchResultVO> expireOpenPaymentOrders(
            @Parameter(description = "本次最多扫描订单数，1-100") @RequestParam(defaultValue = "20") long limit) {
        return R.ok(resourceService.expireOpenPaymentOrders(limit));
    }

    @PostMapping("/tasks/query-processing-orders")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:task:query-processing-orders")
    @Operation(summary = "批量主动查询支付中订单", description = "人工或平台调度触发当前租户支付中订单主动查单任务，逐笔调用通道查单并记录批次审计")
    public R<PaymentTaskDispatchResultVO> queryProcessingPaymentOrders(
            @Parameter(description = "本次最多扫描订单数，1-100") @RequestParam(defaultValue = "20") long limit) {
        return R.ok(resourceService.queryProcessingPaymentOrders(limit));
    }

    @GetMapping("/reconciliations/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:reconciliation:list")
    @Operation(summary = "分页查询对账批次", description = "查询通道账单导入批次、文件摘要、导入人和对账结果")
    public R<PageResult<PaymentReconciliationVO>> pageReconciliations(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(reconciliationService.pageReconciliations(query));
    }

    @GetMapping("/reconciliations/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:reconciliation:query")
    @Operation(summary = "查询对账批次详情", description = "按对账批次 ID 查询账单导入记录和通道账单明细")
    public R<PaymentReconciliationVO> detailReconciliation(@Parameter(description = "对账批次 ID", required = true) @RequestParam Long id) {
        return R.ok(reconciliationService.detailReconciliation(id));
    }

    @GetMapping("/reconciliations/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:reconciliation:list")
    @Operation(summary = "查询对账状态选项", description = "返回对账管理后台筛选使用的状态契约")
    public R<List<PaymentReconciliationStatusVO>> listReconciliationStatuses() {
        return R.ok(reconciliationService.listReconciliationStatuses());
    }

    @PostMapping("/reconciliations/import")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:reconciliation:import")
    @Operation(summary = "导入通道账单", description = "导入通道账单形成对账批次，同一通道、日期、文件摘要只能导入一次，并执行支付成功金额核对")
    public R<PaymentReconciliationVO> importReconciliation(@Valid @RequestBody ImportPaymentReconciliationCommand command) {
        return R.ok(reconciliationService.importReconciliation(command));
    }

    @PostMapping("/reconciliations/mango-pay/virtual/generate")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:reconciliation:import")
    @Operation(summary = "生成芒果支付账单", description = "按账单日期从芒果支付真实支付和退款订单生成通道账单批次，并执行对账核对")
    public R<PaymentReconciliationVO> generateMangoPayVirtualBill(@Valid @RequestBody GenerateMangoPayVirtualBillCommand command) {
        return R.ok(reconciliationService.generateMangoPayVirtualBill(command));
    }

    @PostMapping("/mango-pay/virtual/scenario-controls")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:channel:scenario-control")
    @Operation(summary = "创建芒果支付异常场景控制", description = "控制 MANGO_PAY 通道下一笔支付、查单、退款、退款查询或账单差异场景")
    public R<Long> createMangoPayScenarioControl(@Valid @RequestBody CreateMangoPayScenarioControlCommand command) {
        return R.ok(scenarioControlService.createScenarioControl(command));
    }

    @GetMapping("/differences/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:difference:list")
    @Operation(summary = "分页查询对账差异", description = "查询我方与通道账单不一致的差异单和处理状态")
    public R<PageResult<PaymentDifferenceVO>> pageDifferences(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(resourceService.pageDifferences(query));
    }

    @GetMapping("/differences/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:difference:query")
    @Operation(summary = "查询对账差异详情", description = "按差异单 ID 查询差异原因、处理动作、处理人和凭据")
    public R<PaymentDifferenceVO> detailDifference(@Parameter(description = "对账差异 ID", required = true) @RequestParam Long id) {
        return R.ok(resourceService.detailDifference(id));
    }

    @GetMapping("/differences/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:difference:list")
    @Operation(summary = "查询对账差异处理状态选项", description = "返回差异处理后台筛选使用的处理状态契约")
    public R<List<PaymentDifferenceStatusVO>> listDifferenceStatuses() {
        return R.ok(resourceService.listDifferenceStatuses());
    }

    @GetMapping("/differences/actions")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:difference:handle")
    @Operation(summary = "查询对账差异处理动作选项", description = "返回对账差异后台受控处理动作契约")
    public R<List<PaymentDifferenceActionVO>> listDifferenceActions() {
        return R.ok(resourceService.listDifferenceActions());
    }

    @PostMapping("/differences/handle")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:difference:handle")
    @Operation(summary = "处理对账差异", description = "受控处理对账差异，记录动作、原因、处理人、时间和凭据，不直接修改支付或退款成功状态")
    public R<PaymentDifferenceVO> handleDifference(@Valid @RequestBody HandlePaymentDifferenceCommand command) {
        return R.ok(resourceService.handleDifference(command));
    }

    @GetMapping("/settlement-summaries/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:settlement-summary:list")
    @Operation(summary = "分页查询结算汇总", description = "按日、应用、企业主体和通道查询支付、退款、手续费、净收款汇总")
    public R<PageResult<PaymentSettlementSummaryVO>> pageSettlementSummaries(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(settlementSummaryService.pageSettlementSummaries(query));
    }

    @GetMapping("/settlement-summaries/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:settlement-summary:query")
    @Operation(summary = "查询结算汇总详情", description = "按结算汇总 ID 查询生成、确认、作废和汇总指标")
    public R<PaymentSettlementSummaryVO> detailSettlementSummary(@Parameter(description = "结算汇总 ID", required = true) @RequestParam Long id) {
        return R.ok(settlementSummaryService.detailSettlementSummary(id));
    }

    @GetMapping("/settlement-summaries/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:settlement-summary:list")
    @Operation(summary = "查询结算汇总状态选项", description = "返回结算汇总后台筛选使用的状态契约")
    public R<List<PaymentSettlementSummaryStatusVO>> listSettlementSummaryStatuses() {
        return R.ok(settlementSummaryService.listSettlementSummaryStatuses());
    }

    @PostMapping("/settlement-summaries/generate")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:settlement-summary:generate")
    @Operation(summary = "生成结算汇总", description = "按日期、应用、企业主体和通道生成财务核对汇总，不触发自动付款或会计凭证")
    public R<PaymentSettlementSummaryVO> generateSettlementSummary(@Valid @RequestBody GeneratePaymentSettlementSummaryCommand command) {
        return R.ok(settlementSummaryService.generateSettlementSummary(command));
    }

    @PostMapping("/settlement-summaries/confirm")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:settlement-summary:confirm")
    @Operation(summary = "确认结算汇总", description = "确认前校验对应范围已对账且不存在未处理差异")
    public R<PaymentSettlementSummaryVO> confirmSettlementSummary(@Valid @RequestBody ConfirmPaymentSettlementSummaryCommand command) {
        return R.ok(settlementSummaryService.confirmSettlementSummary(command));
    }

    @PostMapping("/settlement-summaries/void")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:settlement-summary:void")
    @Operation(summary = "作废结算汇总", description = "已确认汇总不可覆盖，修正需先作废再重新生成")
    public R<PaymentSettlementSummaryVO> voidSettlementSummary(@Valid @RequestBody VoidPaymentSettlementSummaryCommand command) {
        return R.ok(settlementSummaryService.voidSettlementSummary(command));
    }

    @GetMapping("/operation-audits/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:operation-audit:list")
    @Operation(summary = "分页查询操作审计", description = "查询支付域关键配置变更、资金相关人工操作和审批处理的审计记录")
    public R<PageResult<PaymentOperationAuditVO>> pageOperationAudits(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(resourceService.pageOperationAudits(query));
    }

    @GetMapping("/channel-capabilities/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:channel:list")
    @Operation(summary = "分页查询通道能力")
    public R<PageResult<PaymentChannelCapabilityVO>> pageChannelCapabilities(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(resourceService.pageChannelCapabilities(query));
    }

    @PostMapping("/security/sensitive-fields/reencrypt")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:security:reencrypt-sensitive")
    @Operation(summary = "重加密历史敏感字段", description = "对当前租户历史明文应用密钥、主体证件号和银行账号执行受控重加密，只返回处理计数")
    public R<PaymentSensitiveFieldReencryptResultVO> reencryptSensitiveFields(
            @Parameter(description = "本轮最多处理记录数，1-1000") @RequestParam(defaultValue = "100") Integer limit) {
        return R.ok(sensitiveFieldReencryptService.reencryptCurrentTenant(limit));
    }

    @GetMapping("/observability/snapshot")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:observability:query")
    @Operation(summary = "查询支付可观测性快照", description = "按当前租户从真实支付表统计支付、退款、回调、通知、对账、异常和证书到期指标，并返回告警项")
    public R<PaymentObservabilitySnapshotVO> observabilitySnapshot() {
        return R.ok(observabilityService.currentSnapshot());
    }
}
