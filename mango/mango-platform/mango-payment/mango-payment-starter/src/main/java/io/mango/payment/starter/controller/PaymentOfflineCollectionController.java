package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentOfflineCollectionApi;
import io.mango.payment.api.command.ConfirmOfflineBankStatementMatchCommand;
import io.mango.payment.api.command.ConfirmOfflineCollectionCommand;
import io.mango.payment.api.command.CreateOfflineRefundCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentOfflineBankStatementBatchStatusVO;
import io.mango.payment.api.vo.PaymentOfflineBankStatementBatchVO;
import io.mango.payment.api.vo.PaymentOfflineBankStatementMatchStatusVO;
import io.mango.payment.api.vo.PaymentOfflineCollectionStatusVO;
import io.mango.payment.api.vo.PaymentOfflineCollectionVO;
import io.mango.payment.api.vo.PaymentOfflineRefundVO;
import io.mango.payment.core.service.PaymentOfflineChannelService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Validated
@RestController
@RequestMapping("/payment/offline-collections")
@RequiredArgsConstructor
@Tag(name = "线下收款", description = "线下转账收款、凭证、确认到账和银行流水导入接口")
public class PaymentOfflineCollectionController implements PaymentOfflineCollectionApi {

    private final PaymentOfflineChannelService offlineChannelService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:list")
    @Operation(summary = "分页查询线下收款", description = "查询线下转账收款单、随机对账码、转账备注、凭证数量和到账状态")
    public R<PageResult<PaymentOfflineCollectionVO>> pageOfflineCollections(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(offlineChannelService.pageOfflineCollections(query));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:query")
    @Operation(summary = "查询线下收款详情", description = "按线下收款 ID 查询收款账户、对账码、关联支付订单和到账状态")
    public R<PaymentOfflineCollectionVO> detailOfflineCollection(@Parameter(description = "线下收款 ID", required = true) @RequestParam Long id) {
        return R.ok(offlineChannelService.detailOfflineCollection(id));
    }

    @Override
    @GetMapping("/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:list")
    @Operation(summary = "查询线下收款状态选项", description = "返回线下收款后台筛选使用的状态契约")
    public R<List<PaymentOfflineCollectionStatusVO>> listOfflineCollectionStatuses() {
        return R.ok(offlineChannelService.listOfflineCollectionStatuses());
    }

    @Override
    @PostMapping("/confirm")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:confirm")
    @Operation(summary = "确认线下收款到账", description = "财务确认线下转账到账后推进线下收款、支付订单和业务订单状态")
    public R<PaymentOfflineCollectionVO> confirmOfflineCollection(@Valid @RequestBody ConfirmOfflineCollectionCommand command) {
        return R.ok(offlineChannelService.confirmCollection(command));
    }

    @Override
    @GetMapping("/bank-statements/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:bank-statement:list")
    @Operation(summary = "分页查询线下银行流水导入批次", description = "查询线下收款通道银行流水 Excel 导入批次、匹配和确认结果")
    public R<PageResult<PaymentOfflineBankStatementBatchVO>> pageOfflineBankStatements(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(offlineChannelService.pageBankStatementBatches(query));
    }

    @Override
    @GetMapping("/bank-statements/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:bank-statement:query")
    @Operation(summary = "查询线下银行流水批次详情", description = "按批次 ID 查询银行流水明细、匹配状态和确认结果")
    public R<PaymentOfflineBankStatementBatchVO> detailOfflineBankStatement(@Parameter(description = "银行流水批次 ID", required = true) @RequestParam Long id) {
        return R.ok(offlineChannelService.detailBankStatementBatch(id));
    }

    @Override
    @GetMapping("/bank-statements/statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:bank-statement:list")
    @Operation(summary = "查询线下银行流水批次状态选项", description = "返回线下银行流水批次后台筛选使用的状态契约")
    public R<List<PaymentOfflineBankStatementBatchStatusVO>> listOfflineBankStatementStatuses() {
        return R.ok(offlineChannelService.listBankStatementBatchStatuses());
    }

    @Override
    @GetMapping("/bank-statements/match-statuses")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:bank-statement:list")
    @Operation(summary = "查询线下银行流水匹配状态选项", description = "返回线下银行流水明细匹配状态契约")
    public R<List<PaymentOfflineBankStatementMatchStatusVO>> listOfflineBankStatementMatchStatuses() {
        return R.ok(offlineChannelService.listBankStatementMatchStatuses());
    }

    @PostMapping("/bank-statements/import")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:bank-statement:import")
    @Operation(summary = "导入线下银行流水 Excel", description = "后端解析银行流水 Excel，落批次和明细，并按对账码、金额和状态生成匹配结果")
    public R<PaymentOfflineBankStatementBatchVO> importOfflineBankStatement(
            @Parameter(description = "银行流水 Excel 文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "文件中心 ID，可为空") @RequestParam(required = false) Long statementFileId) throws IOException {
        return importOfflineBankStatement(file.getBytes(), file.getOriginalFilename(), statementFileId);
    }

    @Override
    public R<PaymentOfflineBankStatementBatchVO> importOfflineBankStatement(
            byte[] fileContent,
            String originalFilename,
            Long statementFileId) throws IOException {
        return R.ok(offlineChannelService.importBankStatement(fileContent, originalFilename, statementFileId));
    }

    @Override
    @PostMapping("/bank-statements/confirm")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:bank-statement:confirm")
    @Operation(summary = "确认线下银行流水匹配到账", description = "财务确认匹配银行流水后推进线下收款、支付订单和业务订单状态")
    public R<PaymentOfflineBankStatementBatchVO> confirmOfflineBankStatementMatch(@Valid @RequestBody ConfirmOfflineBankStatementMatchCommand command) {
        return R.ok(offlineChannelService.confirmBankStatementMatches(command));
    }

    @Override
    @PostMapping("/refund")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:offline-collection:refund")
    @Operation(summary = "创建线下退款", description = "线下收款通道录入退款金额、退款账户和退款凭证，支持部分退款")
    public R<PaymentOfflineRefundVO> createOfflineRefund(@Valid @RequestBody CreateOfflineRefundCommand command) {
        return R.ok(offlineChannelService.createOfflineRefund(command));
    }
}
