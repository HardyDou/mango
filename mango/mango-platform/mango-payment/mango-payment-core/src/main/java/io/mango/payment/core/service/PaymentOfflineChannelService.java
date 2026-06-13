package io.mango.payment.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.ConfirmOfflineBankStatementMatchCommand;
import io.mango.payment.api.command.ConfirmOfflineCollectionCommand;
import io.mango.payment.api.command.CreateOfflineRefundCommand;
import io.mango.payment.api.command.SubmitOfflineTransferVoucherCommand;
import io.mango.payment.api.enums.PaymentOfflineBankStatementBatchStatusEnum;
import io.mango.payment.api.enums.PaymentOfflineBankStatementMatchStatusEnum;
import io.mango.payment.api.enums.PaymentBusinessOrderStatusEnum;
import io.mango.payment.api.enums.PaymentChannelCode;
import io.mango.payment.api.enums.PaymentOfflineCollectionStatusEnum;
import io.mango.payment.api.enums.PaymentOfflineRefundStatusEnum;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentOfflineBankStatementBatchStatusVO;
import io.mango.payment.api.vo.PaymentOfflineBankStatementBatchVO;
import io.mango.payment.api.vo.PaymentOfflineBankStatementItemVO;
import io.mango.payment.api.vo.PaymentOfflineBankStatementMatchStatusVO;
import io.mango.payment.api.vo.PaymentOfflineCollectionStatusVO;
import io.mango.payment.api.vo.PaymentOfflineCollectionVO;
import io.mango.payment.api.vo.PaymentOfflineRefundStatusVO;
import io.mango.payment.api.vo.PaymentOfflineRefundVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentOfflineBankStatementBatchEntity;
import io.mango.payment.core.entity.PaymentOfflineBankStatementItemEntity;
import io.mango.payment.core.entity.PaymentOfflineCollectionEntity;
import io.mango.payment.core.entity.PaymentOfflineCollectionMatchEntity;
import io.mango.payment.core.entity.PaymentOfflineCollectionVoucherEntity;
import io.mango.payment.core.entity.PaymentOfflineRefundEntity;
import io.mango.payment.core.entity.PaymentRefundOrderEntity;
import io.mango.payment.core.entity.PaymentTransactionFlowEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentOfflineBankStatementBatchMapper;
import io.mango.payment.core.mapper.PaymentOfflineBankStatementItemMapper;
import io.mango.payment.core.mapper.PaymentOfflineCollectionMapper;
import io.mango.payment.core.mapper.PaymentOfflineCollectionMatchMapper;
import io.mango.payment.core.mapper.PaymentOfflineCollectionVoucherMapper;
import io.mango.payment.core.mapper.PaymentOfflineRefundMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.mapper.PaymentTransactionFlowMapper;
import io.mango.payment.core.model.Money;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PaymentOfflineChannelService {

    private static final DateTimeFormatter NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String CHANNEL_CODE = PaymentChannelCode.OFFLINE_COLLECTION.name();
    private static final Pattern RECONCILIATION_CODE_PATTERN = Pattern.compile("(?<![0-9A-Z])([0-9A-Z]{4,6})(?![0-9A-Z])");

    private final PaymentOfflineCollectionMapper offlineCollectionMapper;
    private final PaymentOfflineCollectionVoucherMapper offlineCollectionVoucherMapper;
    private final PaymentOfflineBankStatementBatchMapper offlineBankStatementBatchMapper;
    private final PaymentOfflineBankStatementItemMapper offlineBankStatementItemMapper;
    private final PaymentOfflineCollectionMatchMapper offlineCollectionMatchMapper;
    private final PaymentOfflineRefundMapper offlineRefundMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentBusinessOrderMapper businessOrderMapper;
    private final PaymentApplicationMapper applicationMapper;
    private final PaymentRefundOrderMapper refundOrderMapper;
    private final PaymentTransactionFlowMapper transactionFlowMapper;
    private final PaymentOrderStatusFlowService statusFlowService;
    private final PaymentNotificationService notificationService;
    private final PaymentSensitiveValueService sensitiveValueService;
    private final PaymentOperationAuditService auditService;
    private final PaymentNumberService numberService;

    public PageResult<PaymentOfflineCollectionVO> pageOfflineCollections(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String statusCode = PaymentContextSupport.trimToNull(resolved.getStatusCode());
        Long tenantId = PaymentContextSupport.currentTenantId();
        long total = offlineCollectionMapper.countOfflineCollections(tenantId, keyword, statusCode);
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentOfflineCollectionVO> rows = offlineCollectionMapper.selectOfflineCollectionPage(
                tenantId, keyword, statusCode, size, (page - 1) * size);
        rows.forEach(this::fillCollectionSummary);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentOfflineCollectionVO detailOfflineCollection(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "线下收款 ID 不能为空");
        PaymentOfflineCollectionVO vo = offlineCollectionMapper.selectOfflineCollectionDetail(PaymentContextSupport.currentTenantId(), id);
        Require.notNull(vo, PaymentCode.PAYMENT_READONLY_RESOURCE_NOT_FOUND);
        fillCollectionSummary(vo);
        return vo;
    }

    public List<PaymentOfflineCollectionStatusVO> listOfflineCollectionStatuses() {
        return PaymentOfflineCollectionStatusEnum.options().stream().map(status -> {
            PaymentOfflineCollectionStatusVO vo = new PaymentOfflineCollectionStatusVO();
            vo.setStatusCode(status.getCode());
            vo.setStatusName(status.getLabel());
            return vo;
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentOfflineCollectionVO submitTransferVoucher(SubmitOfflineTransferVoucherCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "提交转账凭证命令不能为空");
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentOfflineCollectionEntity collection = offlineCollectionMapper.selectByPayOrderNoForUpdate(tenantId, command.getPayOrderNo());
        Require.notNull(collection, PaymentCode.PAYMENT_OFFLINE_COLLECTION_NOT_FOUND);
        Require.isTrue(CHANNEL_CODE.equals(collection.getChannelCode()), PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID);
        Require.isTrue(isWaitingTransfer(collection.getCollectionStatus()) || isPendingConfirm(collection.getCollectionStatus()),
                PaymentCode.PAYMENT_OFFLINE_COLLECTION_STATE_INVALID.getCode(), "只有待转账或待确认到账的线下收款允许提交凭证");
        long transferAmount = Money.cents(command.getTransferAmount()).toPositiveCents("实际转账金额");
        Require.isTrue(transferAmount == Money.cents(collection.getAmount()).toPositiveCents("线下收款金额"),
                PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "实际转账金额必须等于线下收款金额");
        String voucherFileIds = normalizeFileIds(command.getVoucherFileIds(), "转账凭证");
        int updated = offlineCollectionMapper.submitTransferVoucher(
                tenantId,
                collection.getId(),
                transferAmount,
                voucherFileIds,
                countFileIds(voucherFileIds),
                PaymentContextSupport.trimToNull(command.getSubmitRemark()));
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_OFFLINE_COLLECTION_STATE_INVALID);
        persistVoucherRows(tenantId, collection, voucherFileIds, LocalDateTime.now());
        auditService.record(
                PaymentOperationAuditService.ACTION_SUBMIT_OFFLINE_TRANSFER_VOUCHER,
                PaymentOperationAuditService.RESOURCE_PAYMENT_OFFLINE_COLLECTION,
                String.valueOf(collection.getId()),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return detailOfflineCollection(collection.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentOfflineCollectionVO confirmCollection(ConfirmOfflineCollectionCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "确认线下收款命令不能为空");
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentOfflineCollectionEntity collection = offlineCollectionMapper.selectEntityForUpdate(tenantId, command.getId());
        Require.notNull(collection, PaymentCode.PAYMENT_OFFLINE_COLLECTION_NOT_FOUND);
        Require.isTrue(CHANNEL_CODE.equals(collection.getChannelCode()), PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID);
        Require.isTrue(isWaitingTransfer(collection.getCollectionStatus()) || isPendingConfirm(collection.getCollectionStatus()),
                PaymentCode.PAYMENT_OFFLINE_COLLECTION_STATE_INVALID.getCode(), "只有待转账或待确认到账的线下收款允许确认到账");
        long confirmedAmount = Money.cents(command.getConfirmedAmount()).toPositiveCents("确认到账金额");
        Require.isTrue(confirmedAmount == Money.cents(collection.getAmount()).toPositiveCents("线下收款金额"),
                PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "确认到账金额必须等于线下收款金额");
        LocalDateTime now = LocalDateTime.now();
        confirmCollectionProjection(
                tenantId,
                collection,
                confirmedAmount,
                now,
                PaymentContextSupport.trimToNull(command.getConfirmRemark()),
                collection.getCollectionStatus(),
                PaymentOfflineCollectionStatusEnum.CONFIRMED.getCode(),
                collection.getOfflineCollectionNo(),
                "线下收款通道财务确认到账");
        offlineCollectionVoucherMapper.acceptByCollection(
                tenantId,
                collection.getId(),
                PaymentContextSupport.currentUserId(),
                now);
        auditService.record(
                PaymentOperationAuditService.ACTION_CONFIRM_OFFLINE_COLLECTION,
                PaymentOperationAuditService.RESOURCE_PAYMENT_OFFLINE_COLLECTION,
                String.valueOf(collection.getId()),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return detailOfflineCollection(collection.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentOfflineBankStatementBatchVO importBankStatement(
            byte[] fileContent,
            String originalFileName,
            Long statementFileId) {
        Require.notNull(fileContent, PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "银行流水 Excel 不能为空");
        Require.isTrue(fileContent.length > 0, PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "银行流水 Excel 不能为空");
        String fileName = PaymentContextSupport.trimToNull(originalFileName);
        Require.notBlank(fileName, PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "银行流水文件名不能为空");
        Require.isTrue(isExcelFile(fileName), PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "银行流水只支持 xls/xlsx 文件");
        String fileDigest = sha256(fileContent);
        Long tenantId = PaymentContextSupport.currentTenantId();
        Require.isTrue(offlineBankStatementBatchMapper.countImportedFile(tenantId, fileDigest) == 0,
                PaymentCode.PAYMENT_RECONCILIATION_FILE_DUPLICATED.getCode(), "该银行流水文件已导入");

        LocalDateTime now = LocalDateTime.now();
        List<BankStatementRow> rows = parseBankStatementRows(fileContent);
        Require.notEmpty(rows, PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "银行流水明细不能为空");

        PaymentOfflineBankStatementBatchEntity batch = new PaymentOfflineBankStatementBatchEntity();
        batch.setId(IdWorker.getId());
        batch.setBatchNo(numberService.next(PaymentNumberService.PAY_OFFLINE_BANK_BATCH_NO));
        batch.setBankAccountNoMask(firstNonNull(rows.stream().map(BankStatementRow::bankAccountNoMask).toList()));
        batch.setBankName(firstNonNull(rows.stream().map(BankStatementRow::bankName).toList()));
        batch.setStatementFileId(statementFileId);
        batch.setStatementFileName(fileName);
        batch.setFileDigest(fileDigest);
        batch.setTotalCount(rows.size());
        batch.setMatchedCount(0);
        batch.setConfirmedCount(0);
        batch.setDifferenceCount(0);
        batch.setBatchStatus(PaymentOfflineBankStatementBatchStatusEnum.MATCHED.getCode());
        batch.setImporterId(PaymentContextSupport.currentUserId());
        batch.setImporterName(PaymentContextSupport.currentPrincipalName());
        batch.setImportTime(now);
        batch.setTenantId(tenantId);
        batch.setCreatedBy(PaymentContextSupport.currentUserId());
        batch.setCreatedAt(now);
        batch.setUpdatedBy(PaymentContextSupport.currentUserId());
        batch.setUpdatedAt(now);
        batch.setDelFlag(0);
        offlineBankStatementBatchMapper.insert(batch);

        for (BankStatementRow row : rows) {
            persistStatementItem(tenantId, batch, row, now);
        }
        offlineBankStatementBatchMapper.refreshSummary(tenantId, batch.getId());
        auditService.record(
                PaymentOperationAuditService.ACTION_IMPORT_OFFLINE_BANK_STATEMENT,
                PaymentOperationAuditService.RESOURCE_PAYMENT_OFFLINE_COLLECTION,
                batch.getBatchNo(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return detailBankStatementBatch(batch.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentOfflineBankStatementBatchVO confirmBankStatementMatches(ConfirmOfflineBankStatementMatchCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "确认银行流水匹配命令不能为空");
        Require.notEmpty(command.getItemIds(), PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "银行流水明细 ID 不能为空");
        Long tenantId = PaymentContextSupport.currentTenantId();
        Long batchId = null;
        for (Long itemId : command.getItemIds()) {
            Require.notNull(itemId, PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "银行流水明细 ID 不能为空");
            batchId = confirmBankStatementItem(tenantId, itemId, PaymentContextSupport.trimToNull(command.getConfirmRemark()));
        }
        Require.notNull(batchId, PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "银行流水明细 ID 不能为空");
        offlineBankStatementBatchMapper.refreshSummary(tenantId, batchId);
        auditService.record(
                PaymentOperationAuditService.ACTION_CONFIRM_OFFLINE_BANK_STATEMENT_MATCH,
                PaymentOperationAuditService.RESOURCE_PAYMENT_OFFLINE_COLLECTION,
                String.valueOf(batchId),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return detailBankStatementBatch(batchId);
    }

    public PageResult<PaymentOfflineBankStatementBatchVO> pageBankStatementBatches(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery actualQuery = query == null ? new PaymentConfigPageQuery() : query;
        Long tenantId = PaymentContextSupport.currentTenantId();
        String keyword = PaymentContextSupport.trimToNull(actualQuery.getKeyword());
        String statusCode = PaymentContextSupport.trimToNull(actualQuery.getStatusCode());
        long total = offlineBankStatementBatchMapper.countBatches(tenantId, keyword, statusCode);
        long pageNum = actualQuery.getPage();
        long pageSize = actualQuery.getSize();
        List<PaymentOfflineBankStatementBatchVO> rows = offlineBankStatementBatchMapper.selectBatchPage(
                tenantId, keyword, statusCode, pageSize, (pageNum - 1) * pageSize);
        rows.forEach(this::fillBankStatementBatchSummary);
        return PageResult.of(rows, total, pageNum, pageSize);
    }

    public PaymentOfflineBankStatementBatchVO detailBankStatementBatch(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "银行流水批次 ID 不能为空");
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentOfflineBankStatementBatchVO vo = offlineBankStatementBatchMapper.selectBatchDetail(tenantId, id);
        Require.notNull(vo, PaymentCode.PAYMENT_OFFLINE_COLLECTION_NOT_FOUND);
        fillBankStatementBatchSummary(vo);
        List<PaymentOfflineBankStatementItemVO> items = offlineBankStatementItemMapper.selectItemsByBatch(tenantId, id);
        items.forEach(this::fillBankStatementItemSummary);
        vo.setItems(items);
        return vo;
    }

    public List<PaymentOfflineBankStatementBatchStatusVO> listBankStatementBatchStatuses() {
        return PaymentOfflineBankStatementBatchStatusEnum.options().stream().map(status -> {
            PaymentOfflineBankStatementBatchStatusVO vo = new PaymentOfflineBankStatementBatchStatusVO();
            vo.setCode(status.getCode());
            vo.setLabel(status.getLabel());
            return vo;
        }).toList();
    }

    public List<PaymentOfflineBankStatementMatchStatusVO> listBankStatementMatchStatuses() {
        return PaymentOfflineBankStatementMatchStatusEnum.options().stream().map(status -> {
            PaymentOfflineBankStatementMatchStatusVO vo = new PaymentOfflineBankStatementMatchStatusVO();
            vo.setCode(status.getCode());
            vo.setLabel(status.getLabel());
            return vo;
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentOfflineRefundVO createOfflineRefund(CreateOfflineRefundCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_OFFLINE_REFUND_INVALID.getCode(), "创建线下退款命令不能为空");
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentOfflineCollectionEntity collection = offlineCollectionMapper.selectEntityForUpdate(tenantId, command.getOfflineCollectionId());
        Require.notNull(collection, PaymentCode.PAYMENT_OFFLINE_COLLECTION_NOT_FOUND);
        Require.isTrue(CHANNEL_CODE.equals(collection.getChannelCode()), PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID);
        Require.isTrue(PaymentOfflineCollectionStatusEnum.CONFIRMED.getCode().equals(collection.getCollectionStatus())
                        || PaymentOfflineCollectionStatusEnum.RECONCILED.getCode().equals(collection.getCollectionStatus()),
                PaymentCode.PAYMENT_OFFLINE_COLLECTION_STATE_INVALID.getCode(), "只有已确认到账的线下收款允许退款");
        long refundAmount = Money.cents(command.getRefundAmount()).toPositiveCents("退款金额");
        long refundedAmount = normalizedAmount(refundOrderMapper.sumOccupyingRefundAmount(tenantId, collection.getPaymentOrderId()));
        long paidAmount = Money.cents(collection.getConfirmedAmount() == null ? collection.getAmount() : collection.getConfirmedAmount())
                .toPositiveCents("已收款金额");
        Require.isTrue(refundAmount <= paidAmount - refundedAmount, PaymentCode.PAYMENT_REFUND_AMOUNT_EXCEEDED);
        String voucherFileIds = normalizeFileIds(command.getRefundVoucherFileIds(), "退款凭证");
        LocalDateTime now = LocalDateTime.now();

        PaymentBusinessOrderEntity businessOrder = businessOrderMapper.selectCashierBusinessOrder(tenantId, collection.getBusinessOrderId());
        Require.notNull(businessOrder, PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_FOUND);
        String offlineRefundNo = numberService.next(PaymentNumberService.PAY_OFFLINE_REFUND_NO);
        String refundOrderNo = numberService.next(PaymentNumberService.PAY_REFUND_ORDER_NO);
        PaymentRefundOrderEntity refundOrder = new PaymentRefundOrderEntity();
        refundOrder.setRefundOrderNo(refundOrderNo);
        refundOrder.setBizRefundNo(offlineRefundNo);
        refundOrder.setPaymentOrderId(collection.getPaymentOrderId());
        refundOrder.setChannelRefundNo(offlineRefundNo);
        refundOrder.setRefundAmount(refundAmount);
        refundOrder.setReason(PaymentContextSupport.trimToNull(command.getReason()));
        refundOrder.setStatus(PaymentRefundOrderStatusEnum.SUCCESS.getCode());
        refundOrder.setRefundTime(now);
        refundOrder.setTenantId(tenantId);
        refundOrder.setCreatedBy(PaymentContextSupport.currentUserId());
        refundOrder.setCreatedAt(now);
        refundOrder.setUpdatedBy(PaymentContextSupport.currentUserId());
        refundOrder.setUpdatedAt(now);
        refundOrderMapper.insert(refundOrder);

        PaymentOfflineRefundEntity refund = new PaymentOfflineRefundEntity();
        refund.setOfflineRefundNo(offlineRefundNo);
        refund.setOfflineCollectionId(collection.getId());
        refund.setOfflineCollectionNo(collection.getOfflineCollectionNo());
        refund.setRefundOrderId(refundOrder.getId());
        refund.setPaymentOrderId(collection.getPaymentOrderId());
        refund.setPayOrderNo(collection.getPayOrderNo());
        refund.setBusinessOrderId(collection.getBusinessOrderId());
        refund.setBizOrderNo(collection.getBizOrderNo());
        refund.setChannelId(collection.getChannelId());
        refund.setChannelCode(CHANNEL_CODE);
        refund.setRefundAmount(refundAmount);
        refund.setCurrency(collection.getCurrency());
        refund.setRefundAccountName(PaymentContextSupport.trimToNull(command.getRefundAccountName()));
        refund.setRefundAccountNoMask(sensitiveValueService.mask(command.getRefundAccountNo(), 4, 4));
        refund.setRefundBankName(PaymentContextSupport.trimToNull(command.getRefundBankName()));
        refund.setRefundVoucherFileIds(voucherFileIds);
        refund.setRefundVoucherCount(countFileIds(voucherFileIds));
        refund.setReason(PaymentContextSupport.trimToNull(command.getReason()));
        refund.setRemark(PaymentContextSupport.trimToNull(command.getRemark()));
        refund.setRefundStatus(PaymentOfflineRefundStatusEnum.REFUNDED.getCode());
        refund.setRefundedTime(now);
        refund.setOperatorId(PaymentContextSupport.currentUserId());
        refund.setOperatorName(PaymentContextSupport.currentPrincipalName());
        refund.setTenantId(tenantId);
        refund.setCreatedBy(PaymentContextSupport.currentUserId());
        refund.setCreatedAt(now);
        refund.setUpdatedBy(PaymentContextSupport.currentUserId());
        refund.setUpdatedAt(now);
        refund.setDelFlag(0);
        offlineRefundMapper.insert(refund);

        int businessUpdated = businessOrderMapper.updateRefundProgress(tenantId, collection.getBusinessOrderId(), refundAmount);
        Require.isTrue(businessUpdated == 1, PaymentCode.PAYMENT_REFUND_AMOUNT_EXCEEDED);
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_BUSINESS,
                collection.getBusinessOrderId(),
                businessOrder.getBizOrderNo(),
                businessOrder.getStatus(),
                nextBusinessRefundStatus(businessOrder, refundAmount),
                PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK,
                refund.getOfflineRefundNo(),
                now,
                "线下收款通道退款凭证确认退款完成");
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_REFUND,
                refundOrder.getId(),
                refundOrderNo,
                null,
                PaymentRefundOrderStatusEnum.REFUNDING.getCode(),
                PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK,
                refund.getOfflineRefundNo(),
                now,
                "线下收款通道创建统一退款订单");
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_REFUND,
                refundOrder.getId(),
                refundOrderNo,
                PaymentRefundOrderStatusEnum.REFUNDING.getCode(),
                PaymentRefundOrderStatusEnum.SUCCESS.getCode(),
                PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK,
                refund.getOfflineRefundNo(),
                now,
                "线下收款通道凭证确认退款成功");
        PaymentTransactionFlowEntity flow = new PaymentTransactionFlowEntity();
        flow.setFlowNo(numberService.next(PaymentNumberService.PAY_REFUND_FLOW_NO));
        flow.setBusinessOrderId(collection.getBusinessOrderId());
        flow.setPaymentOrderId(collection.getPaymentOrderId());
        flow.setRefundOrderId(refundOrder.getId());
        flow.setFlowType("REFUND_SUCCESS");
        flow.setAmount(refundAmount);
        flow.setTenantId(tenantId);
        flow.setCreatedBy(PaymentContextSupport.currentUserId());
        flow.setCreatedAt(now);
        flow.setUpdatedBy(PaymentContextSupport.currentUserId());
        flow.setUpdatedAt(now);
        transactionFlowMapper.insert(flow);
        notifyRefundTerminal(tenantId, businessOrder, refundOrder.getId());
        auditService.record(
                PaymentOperationAuditService.ACTION_CREATE_OFFLINE_REFUND,
                PaymentOperationAuditService.RESOURCE_PAYMENT_OFFLINE_REFUND,
                String.valueOf(refund.getId()),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return detailOfflineRefund(refund.getId());
    }

    public PageResult<PaymentOfflineRefundVO> pageOfflineRefunds(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery actualQuery = query == null ? new PaymentConfigPageQuery() : query;
        Long tenantId = PaymentContextSupport.currentTenantId();
        String keyword = PaymentContextSupport.trimToNull(actualQuery.getKeyword());
        String statusCode = PaymentContextSupport.trimToNull(actualQuery.getStatusCode());
        long total = offlineRefundMapper.countOfflineRefunds(tenantId, keyword, statusCode);
        long pageNum = actualQuery.getPage();
        long pageSize = actualQuery.getSize();
        long offset = (pageNum - 1) * pageSize;
        List<PaymentOfflineRefundVO> rows = offlineRefundMapper.selectOfflineRefundPage(tenantId, keyword, statusCode, pageSize, offset);
        rows.forEach(this::fillRefundSummary);
        return PageResult.of(rows, total, pageNum, pageSize);
    }

    public PaymentOfflineRefundVO detailOfflineRefund(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_OFFLINE_REFUND_INVALID.getCode(), "线下退款 ID 不能为空");
        PaymentOfflineRefundVO vo = offlineRefundMapper.selectOfflineRefundDetail(PaymentContextSupport.currentTenantId(), id);
        Require.notNull(vo, PaymentCode.PAYMENT_OFFLINE_REFUND_NOT_FOUND);
        fillRefundSummary(vo);
        return vo;
    }

    public List<PaymentOfflineRefundStatusVO> listOfflineRefundStatuses() {
        return PaymentOfflineRefundStatusEnum.options().stream().map(status -> {
            PaymentOfflineRefundStatusVO vo = new PaymentOfflineRefundStatusVO();
            vo.setCode(status.getCode());
            vo.setLabel(status.getLabel());
            return vo;
        }).toList();
    }

    private void fillCollectionSummary(PaymentOfflineCollectionVO vo) {
        vo.setCollectionStatusName(PaymentOfflineCollectionStatusEnum.labelOf(vo.getCollectionStatus()));
    }

    private void confirmCollectionProjection(
            Long tenantId,
            PaymentOfflineCollectionEntity collection,
            long confirmedAmount,
            LocalDateTime eventTime,
            String confirmRemark,
            String requiredStatus,
            String nextStatus,
            String channelTradeNo,
            String flowRemark) {
        PaymentBusinessOrderEntity businessOrder = businessOrderMapper.selectCashierBusinessOrder(tenantId, collection.getBusinessOrderId());
        Require.notNull(businessOrder, PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_FOUND);
        int collectionUpdated = offlineCollectionMapper.confirmCollection(
                tenantId,
                collection.getId(),
                confirmedAmount,
                eventTime,
                PaymentContextSupport.currentUserId(),
                PaymentContextSupport.currentPrincipalName(),
                confirmRemark,
                nextStatus,
                requiredStatus);
        Require.isTrue(collectionUpdated == 1, PaymentCode.PAYMENT_OFFLINE_COLLECTION_STATE_INVALID);

        int paymentUpdated = paymentOrderMapper.updateOfflineCollectionSuccess(
                tenantId,
                collection.getPaymentOrderId(),
                eventTime,
                channelTradeNo);
        Require.isTrue(paymentUpdated == 1, PaymentCode.PAYMENT_ORDER_STATE_INVALID);
        int businessUpdated = businessOrderMapper.markCashierPaySuccess(tenantId, collection.getBusinessOrderId(), confirmedAmount);
        Require.isTrue(businessUpdated == 1, PaymentCode.PAYMENT_BUSINESS_ORDER_STATE_CHANGED);
        recordPaymentProjectionFlows(tenantId, collection, businessOrder, confirmedAmount, eventTime, channelTradeNo, flowRemark);
        notifyPaymentTerminal(tenantId, collection.getPaymentOrderId(), businessOrder);
    }

    private Long confirmBankStatementItem(Long tenantId, Long itemId, String confirmRemark) {
        PaymentOfflineBankStatementItemEntity item = offlineBankStatementItemMapper.selectEntityForUpdate(tenantId, itemId);
        Require.notNull(item, PaymentCode.PAYMENT_OFFLINE_COLLECTION_NOT_FOUND);
        Require.isTrue(PaymentOfflineBankStatementMatchStatusEnum.MATCHED_PENDING_CONFIRM.getCode().equals(item.getMatchStatus()),
                PaymentCode.PAYMENT_OFFLINE_COLLECTION_STATE_INVALID.getCode(), "只有已匹配待确认的银行流水允许确认到账");
        Require.notNull(item.getMatchedOfflineCollectionId(),
                PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "银行流水未匹配线下收款单");
        PaymentOfflineCollectionEntity collection = offlineCollectionMapper.selectEntityForUpdate(
                tenantId,
                item.getMatchedOfflineCollectionId());
        Require.notNull(collection, PaymentCode.PAYMENT_OFFLINE_COLLECTION_NOT_FOUND);
        Require.isTrue(CHANNEL_CODE.equals(collection.getChannelCode()), PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID);
        Require.isTrue(isWaitingTransfer(collection.getCollectionStatus()) || isPendingConfirm(collection.getCollectionStatus()),
                PaymentCode.PAYMENT_OFFLINE_COLLECTION_STATE_INVALID.getCode(), "线下收款单当前状态不允许通过银行流水确认");
        Require.isTrue(Money.cents(item.getAmount()).toPositiveCents("银行流水金额")
                        == Money.cents(collection.getAmount()).toPositiveCents("线下收款金额"),
                PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "银行流水金额必须等于线下收款金额");
        LocalDateTime now = LocalDateTime.now();
        confirmCollectionProjection(
                tenantId,
                collection,
                item.getAmount(),
                now,
                confirmRemark,
                collection.getCollectionStatus(),
                PaymentOfflineCollectionStatusEnum.RECONCILED.getCode(),
                item.getBankStatementNo(),
                "线下收款银行流水匹配确认到账");
        int itemUpdated = offlineBankStatementItemMapper.markConfirmed(
                tenantId,
                item.getId(),
                now,
                PaymentContextSupport.currentUserId(),
                PaymentContextSupport.currentPrincipalName(),
                confirmRemark);
        Require.isTrue(itemUpdated == 1, PaymentCode.PAYMENT_OFFLINE_COLLECTION_STATE_INVALID);
        int matchUpdated = offlineCollectionMatchMapper.markConfirmed(
                tenantId,
                item.getId(),
                now,
                PaymentContextSupport.currentUserId(),
                PaymentContextSupport.currentPrincipalName());
        Require.isTrue(matchUpdated == 1, PaymentCode.PAYMENT_OFFLINE_COLLECTION_STATE_INVALID);
        return item.getBatchId();
    }

    private void persistVoucherRows(
            Long tenantId,
            PaymentOfflineCollectionEntity collection,
            String voucherFileIds,
            LocalDateTime now) {
        for (String fileId : splitFileIds(voucherFileIds)) {
            PaymentOfflineCollectionVoucherEntity voucher = new PaymentOfflineCollectionVoucherEntity();
            voucher.setId(IdWorker.getId());
            voucher.setOfflineCollectionId(collection.getId());
            voucher.setOfflineCollectionNo(collection.getOfflineCollectionNo());
            voucher.setPayOrderNo(collection.getPayOrderNo());
            voucher.setVoucherFileId(fileId);
            voucher.setUploadSource("CASHIER");
            voucher.setUploaderId(PaymentContextSupport.currentUserId());
            voucher.setUploaderName(PaymentContextSupport.currentPrincipalName());
            voucher.setUploadTime(now);
            voucher.setReviewStatus("SUBMITTED");
            voucher.setTenantId(tenantId);
            voucher.setCreatedBy(PaymentContextSupport.currentUserId());
            voucher.setCreatedAt(now);
            voucher.setUpdatedBy(PaymentContextSupport.currentUserId());
            voucher.setUpdatedAt(now);
            voucher.setDelFlag(0);
            offlineCollectionVoucherMapper.insert(voucher);
        }
    }

    private void persistStatementItem(
            Long tenantId,
            PaymentOfflineBankStatementBatchEntity batch,
            BankStatementRow row,
            LocalDateTime now) {
        MatchResult match = matchStatementRow(tenantId, row);
        PaymentOfflineBankStatementItemEntity item = new PaymentOfflineBankStatementItemEntity();
        item.setId(IdWorker.getId());
        item.setBatchId(batch.getId());
        item.setBatchNo(batch.getBatchNo());
        item.setRowNo(row.rowNo());
        item.setBankStatementNo(row.bankStatementNo());
        item.setBankAccountNoMask(row.bankAccountNoMask());
        item.setBankName(row.bankName());
        item.setTradeTime(row.tradeTime());
        item.setTradeDate(row.tradeTime().toLocalDate());
        item.setAmount(row.amount());
        item.setCurrency(row.currency());
        item.setCounterpartyName(row.counterpartyName());
        item.setCounterpartyAccountNoMask(row.counterpartyAccountNoMask());
        item.setSummary(row.summary());
        item.setRemark(row.remark());
        item.setReconciliationCode(row.reconciliationCode());
        item.setMatchedOfflineCollectionId(match.collection() == null ? null : match.collection().getId());
        item.setMatchedOfflineCollectionNo(match.collection() == null ? null : match.collection().getOfflineCollectionNo());
        item.setMatchedPayOrderNo(match.collection() == null ? null : match.collection().getPayOrderNo());
        item.setMatchStatus(match.status());
        item.setMatchMessage(match.message());
        item.setTenantId(tenantId);
        item.setCreatedBy(PaymentContextSupport.currentUserId());
        item.setCreatedAt(now);
        item.setUpdatedBy(PaymentContextSupport.currentUserId());
        item.setUpdatedAt(now);
        item.setDelFlag(0);
        offlineBankStatementItemMapper.insert(item);
        if (match.collection() != null) {
            persistCollectionMatch(tenantId, item, match, now);
        }
    }

    private void persistCollectionMatch(
            Long tenantId,
            PaymentOfflineBankStatementItemEntity item,
            MatchResult match,
            LocalDateTime now) {
        PaymentOfflineCollectionEntity collection = match.collection();
        PaymentOfflineCollectionMatchEntity entity = new PaymentOfflineCollectionMatchEntity();
        entity.setId(IdWorker.getId());
        entity.setOfflineCollectionId(collection.getId());
        entity.setOfflineCollectionNo(collection.getOfflineCollectionNo());
        entity.setBankStatementItemId(item.getId());
        entity.setBankStatementNo(item.getBankStatementNo());
        entity.setPayOrderNo(collection.getPayOrderNo());
        entity.setMatchRule("RECONCILIATION_CODE_AMOUNT");
        entity.setMatchStatus(match.status());
        entity.setDifferenceType(match.differenceType());
        entity.setMatchMessage(match.message());
        entity.setTenantId(tenantId);
        entity.setCreatedBy(PaymentContextSupport.currentUserId());
        entity.setCreatedAt(now);
        entity.setUpdatedBy(PaymentContextSupport.currentUserId());
        entity.setUpdatedAt(now);
        entity.setDelFlag(0);
        offlineCollectionMatchMapper.insert(entity);
    }

    private MatchResult matchStatementRow(Long tenantId, BankStatementRow row) {
        if (offlineBankStatementItemMapper.countExistingStatement(
                tenantId,
                row.bankAccountNoMask(),
                row.bankStatementNo(),
                row.tradeTime().toLocalDate()) > 0) {
            return new MatchResult(
                    PaymentOfflineBankStatementMatchStatusEnum.DUPLICATED_STATEMENT.getCode(),
                    "同一收款账号、银行流水号和交易日期已导入",
                    null,
                    "DUPLICATED_STATEMENT");
        }
        if (row.reconciliationCode() == null) {
            return new MatchResult(
                    PaymentOfflineBankStatementMatchStatusEnum.UNMATCHED.getCode(),
                    "银行流水备注中未识别到转账备注识别码",
                    null,
                    "NO_RECONCILIATION_CODE");
        }
        PaymentOfflineCollectionEntity collection = offlineCollectionMapper.selectByReconciliationCodeForUpdate(
                tenantId,
                row.reconciliationCode());
        if (collection == null) {
            return new MatchResult(
                    PaymentOfflineBankStatementMatchStatusEnum.UNMATCHED.getCode(),
                    "未找到转账备注识别码对应的线下收款单",
                    null,
                    "COLLECTION_NOT_FOUND");
        }
        if (!isWaitingTransfer(collection.getCollectionStatus()) && !isPendingConfirm(collection.getCollectionStatus())) {
            return new MatchResult(
                    PaymentOfflineBankStatementMatchStatusEnum.COLLECTION_STATE_INVALID.getCode(),
                    "线下收款单当前状态不允许通过银行流水确认",
                    collection,
                    "COLLECTION_STATE_INVALID");
        }
        if (!Objects.equals(collection.getAmount(), row.amount())) {
            return new MatchResult(
                    PaymentOfflineBankStatementMatchStatusEnum.AMOUNT_MISMATCH.getCode(),
                    "银行流水金额与线下收款金额不一致",
                    collection,
                    "AMOUNT_MISMATCH");
        }
        return new MatchResult(
                PaymentOfflineBankStatementMatchStatusEnum.MATCHED_PENDING_CONFIRM.getCode(),
                "按转账备注识别码和金额唯一匹配，等待财务确认",
                collection,
                null);
    }

    private List<BankStatementRow> parseBankStatementRows(byte[] fileContent) {
        try (InputStream input = new BufferedInputStream(new ByteArrayInputStream(fileContent));
             var workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            Require.notNull(sheet, PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "银行流水 Excel 没有工作表");
            Row header = sheet.getRow(sheet.getFirstRowNum());
            Require.notNull(header, PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "银行流水 Excel 缺少表头");
            Map<String, Integer> columns = headerColumns(header);
            List<BankStatementRow> rows = new ArrayList<>();
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                rows.add(parseBankStatementRow(row, columns));
            }
            return rows;
        } catch (IOException ex) {
            return Require.fail(PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "银行流水 Excel 读取失败");
        }
    }

    private BankStatementRow parseBankStatementRow(Row row, Map<String, Integer> columns) {
        int rowNo = row.getRowNum() + 1;
        String bankStatementNo = requiredCell(row, columns, rowNo, "银行流水号", "流水号", "交易流水号");
        LocalDateTime tradeTime = parseTradeTime(row, columns, rowNo, "交易时间", "到账时间", "入账时间");
        long amount = parseYuanAmount(requiredCell(row, columns, rowNo, "收入金额", "金额", "贷方金额", "到账金额"), rowNo);
        String summary = optionalCell(row, columns, "摘要", "用途", "交易摘要");
        String remark = optionalCell(row, columns, "备注", "附言", "转账备注");
        String bankAccountNo = optionalCell(row, columns, "收款账号", "本方账号", "银行账号");
        String counterpartyAccountNo = optionalCell(row, columns, "对方账号", "付款账号");
        String combinedRemark = String.join(" ", List.of(nullToEmpty(summary), nullToEmpty(remark)));
        return new BankStatementRow(
                rowNo,
                bankStatementNo,
                sensitiveValueService.mask(bankAccountNo, 4, 4),
                optionalCell(row, columns, "开户行", "银行名称", "收款开户行"),
                tradeTime,
                amount,
                "CNY",
                optionalCell(row, columns, "对方户名", "付款户名", "付款方"),
                sensitiveValueService.mask(counterpartyAccountNo, 4, 4),
                summary,
                remark,
                extractReconciliationCode(combinedRemark));
    }

    private Map<String, Integer> headerColumns(Row header) {
        java.util.LinkedHashMap<String, Integer> columns = new java.util.LinkedHashMap<>();
        DataFormatter formatter = new DataFormatter(Locale.CHINA);
        for (Cell cell : header) {
            String name = PaymentContextSupport.trimToNull(formatter.formatCellValue(cell));
            if (name != null) {
                columns.put(name, cell.getColumnIndex());
            }
        }
        return columns;
    }

    private boolean isBlankRow(Row row) {
        DataFormatter formatter = new DataFormatter(Locale.CHINA);
        for (Cell cell : row) {
            if (PaymentContextSupport.trimToNull(formatter.formatCellValue(cell)) != null) {
                return false;
            }
        }
        return true;
    }

    private String requiredCell(Row row, Map<String, Integer> columns, int rowNo, String... names) {
        String value = optionalCell(row, columns, names);
        Require.notBlank(value, PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "第 " + rowNo + " 行缺少 " + names[0]);
        return value;
    }

    private String optionalCell(Row row, Map<String, Integer> columns, String... names) {
        Integer index = columnIndex(columns, names);
        if (index == null) {
            return null;
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        DataFormatter formatter = new DataFormatter(Locale.CHINA);
        return PaymentContextSupport.trimToNull(formatter.formatCellValue(cell));
    }

    private Integer columnIndex(Map<String, Integer> columns, String... names) {
        for (String name : names) {
            Integer index = columns.get(name);
            if (index != null) {
                return index;
            }
        }
        return null;
    }

    private LocalDateTime parseTradeTime(Row row, Map<String, Integer> columns, int rowNo, String... names) {
        Integer index = columnIndex(columns, names);
        Require.notNull(index, PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "银行流水 Excel 缺少 " + names[0] + " 表头");
        Cell cell = row.getCell(index);
        Require.notNull(cell, PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "第 " + rowNo + " 行缺少 " + names[0]);
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue();
        }
        String value = PaymentContextSupport.trimToNull(new DataFormatter(Locale.CHINA).formatCellValue(cell));
        Require.notBlank(value, PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "第 " + rowNo + " 行缺少 " + names[0]);
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
            } catch (DateTimeParseException ex) {
                return Require.fail(PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "第 " + rowNo + " 行交易时间格式不正确");
            }
        }
    }

    private long parseYuanAmount(String value, int rowNo) {
        try {
            return new BigDecimal(value.replace(",", ""))
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
            Require.fail(PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "第 " + rowNo + " 行金额格式不正确");
            return 0L;
        }
    }

    private String extractReconciliationCode(String value) {
        Matcher matcher = RECONCILIATION_CODE_PATTERN.matcher(value == null ? "" : value.toUpperCase(Locale.ROOT));
        return matcher.find() ? matcher.group(1) : null;
    }

    private String sha256(byte[] fileContent) {
        try (InputStream input = new ByteArrayInputStream(fileContent)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                digestInput.transferTo(OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException ex) {
            return Require.fail(PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), "银行流水文件摘要计算失败");
        }
    }

    private boolean isExcelFile(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".xls") || lower.endsWith(".xlsx");
    }

    private String firstNonNull(List<String> values) {
        return values.stream().filter(Objects::nonNull).findFirst().orElse(null);
    }

    private void fillBankStatementBatchSummary(PaymentOfflineBankStatementBatchVO vo) {
        vo.setBatchStatusName(PaymentOfflineBankStatementBatchStatusEnum.labelOf(vo.getBatchStatus()));
    }

    private void fillBankStatementItemSummary(PaymentOfflineBankStatementItemVO vo) {
        vo.setMatchStatusName(PaymentOfflineBankStatementMatchStatusEnum.labelOf(vo.getMatchStatus()));
    }

    private void recordPaymentProjectionFlows(
            Long tenantId,
            PaymentOfflineCollectionEntity collection,
            PaymentBusinessOrderEntity businessOrder,
            long confirmedAmount,
            LocalDateTime eventTime,
            String triggerNo,
            String flowRemark) {
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT,
                collection.getPaymentOrderId(),
                collection.getPayOrderNo(),
                PaymentOrderStatusEnum.PAYING.getCode(),
                PaymentOrderStatusEnum.SUCCESS.getCode(),
                PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK,
                triggerNo,
                eventTime,
                flowRemark);
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_BUSINESS,
                collection.getBusinessOrderId(),
                businessOrder.getBizOrderNo(),
                PaymentBusinessOrderStatusEnum.PAYING.getCode(),
                PaymentBusinessOrderStatusEnum.PAID.getCode(),
                PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK,
                triggerNo,
                eventTime,
                flowRemark);
        PaymentTransactionFlowEntity flow = new PaymentTransactionFlowEntity();
        flow.setFlowNo(numberService.next(PaymentNumberService.PAY_FLOW_NO));
        flow.setBusinessOrderId(collection.getBusinessOrderId());
        flow.setPaymentOrderId(collection.getPaymentOrderId());
        flow.setRefundOrderId(null);
        flow.setFlowType("PAY_SUCCESS");
        flow.setAmount(confirmedAmount);
        flow.setTenantId(tenantId);
        flow.setCreatedBy(PaymentContextSupport.currentUserId());
        flow.setCreatedAt(eventTime);
        flow.setUpdatedBy(PaymentContextSupport.currentUserId());
        flow.setUpdatedAt(eventTime);
        transactionFlowMapper.insert(flow);
    }

    private void notifyPaymentTerminal(Long tenantId, Long paymentOrderId, PaymentBusinessOrderEntity businessOrder) {
        PaymentApplication application = applicationMapper.selectOne(new LambdaQueryWrapper<PaymentApplication>()
                .eq(PaymentApplication::getTenantId, tenantId)
                .eq(PaymentApplication::getAppId, businessOrder.getAppCode())
                .eq(PaymentApplication::getStatus, 1));
        Require.notNull(application, PaymentCode.PAYMENT_APPLICATION_NOT_FOUND);
        PaymentOrderVO paymentOrder = paymentOrderMapper.selectPaymentOrderById(tenantId, paymentOrderId);
        Require.notNull(paymentOrder, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        notificationService.notifyPaymentAfterCommit(application, businessOrder, paymentOrder);
    }

    private void notifyRefundTerminal(Long tenantId, PaymentBusinessOrderEntity businessOrder, Long refundOrderId) {
        PaymentApplication application = applicationMapper.selectOne(new LambdaQueryWrapper<PaymentApplication>()
                .eq(PaymentApplication::getTenantId, tenantId)
                .eq(PaymentApplication::getAppId, businessOrder.getAppCode())
                .eq(PaymentApplication::getStatus, 1));
        Require.notNull(application, PaymentCode.PAYMENT_APPLICATION_NOT_FOUND);
        var refundOrder = refundOrderMapper.selectRefundOrderDetail(tenantId, refundOrderId);
        Require.notNull(refundOrder, PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND);
        notificationService.notifyRefundAfterCommit(application, businessOrder, refundOrder);
    }

    private String normalizeFileIds(String fileIds, String fieldName) {
        String normalized = PaymentContextSupport.trimToNull(fileIds);
        Require.notBlank(normalized, PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), fieldName + "不能为空");
        List<String> items = Arrays.stream(normalized.split(","))
                .map(PaymentContextSupport::trimToNull)
                .filter(item -> item != null)
                .toList();
        Require.isTrue(!items.isEmpty(), PaymentCode.PAYMENT_OFFLINE_COLLECTION_INVALID.getCode(), fieldName + "不能为空");
        return String.join(",", items);
    }

    private int countFileIds(String fileIds) {
        return splitFileIds(fileIds).size();
    }

    private List<String> splitFileIds(String fileIds) {
        return Arrays.stream(fileIds.split(","))
                .map(PaymentContextSupport::trimToNull)
                .filter(item -> item != null)
                .toList();
    }

    private boolean isWaitingTransfer(String status) {
        return PaymentOfflineCollectionStatusEnum.WAITING_TRANSFER.getCode().equals(status);
    }

    private boolean isPendingConfirm(String status) {
        return PaymentOfflineCollectionStatusEnum.PENDING_CONFIRM.getCode().equals(status);
    }

    private String nextBusinessRefundStatus(PaymentBusinessOrderEntity businessOrder, Long refundAmount) {
        long paidAmount = businessOrder.getPaidAmount() == null ? 0L : businessOrder.getPaidAmount();
        long refundedAmount = businessOrder.getRefundedAmount() == null ? 0L : businessOrder.getRefundedAmount();
        long nextRefundedAmount = refundedAmount + (refundAmount == null ? 0L : refundAmount);
        if (nextRefundedAmount >= paidAmount) {
            return PaymentBusinessOrderStatusEnum.REFUNDED.getCode();
        }
        return PaymentBusinessOrderStatusEnum.PARTIAL_REFUNDED.getCode();
    }

    private long normalizedAmount(Long amount) {
        return amount == null ? 0L : amount;
    }

    private void fillRefundSummary(PaymentOfflineRefundVO vo) {
        vo.setRefundStatusName(PaymentOfflineRefundStatusEnum.labelOf(vo.getRefundStatus()));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record BankStatementRow(
            int rowNo,
            String bankStatementNo,
            String bankAccountNoMask,
            String bankName,
            LocalDateTime tradeTime,
            long amount,
            String currency,
            String counterpartyName,
            String counterpartyAccountNoMask,
            String summary,
            String remark,
            String reconciliationCode) {
    }

    private record MatchResult(
            String status,
            String message,
            PaymentOfflineCollectionEntity collection,
            String differenceType) {
    }
}
