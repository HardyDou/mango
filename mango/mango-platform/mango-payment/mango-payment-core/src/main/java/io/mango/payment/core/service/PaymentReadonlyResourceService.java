package io.mango.payment.core.service;

import io.mango.common.vo.PageResult;
import io.mango.common.result.Require;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.CreatePaymentBusinessOrderCommand;
import io.mango.payment.api.command.HandlePaymentDifferenceCommand;
import io.mango.payment.api.command.HandlePaymentExceptionOrderCommand;
import io.mango.payment.api.command.QueryPaymentRefundOrderCommand;
import io.mango.payment.api.command.RetryPaymentNotificationRecordCommand;
import io.mango.payment.api.enums.PaymentBusinessOrderStatusEnum;
import io.mango.payment.api.enums.PaymentOfflineCollectionStatusEnum;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
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
import io.mango.payment.api.vo.PaymentOfflineCollectionStatusVO;
import io.mango.payment.api.vo.PaymentOfflineCollectionVO;
import io.mango.payment.api.vo.PaymentOperationAuditVO;
import io.mango.payment.api.vo.PaymentOrderSyncStatusVO;
import io.mango.payment.api.vo.PaymentOrderStatusFlowVO;
import io.mango.payment.api.vo.PaymentOrderStatusVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.PaymentRefundOrderStatusVO;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.api.vo.PaymentTaskDispatchResultVO;
import io.mango.payment.api.vo.PaymentTransactionFlowVO;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentExceptionOrderEntity;
import io.mango.payment.core.entity.PaymentDifferenceEntity;
import io.mango.payment.core.entity.PaymentEnterpriseSubject;
import io.mango.payment.core.entity.PaymentNotificationRecordEntity;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.entity.PaymentTransactionFlowEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentChannelCapabilityMapper;
import io.mango.payment.core.mapper.PaymentDifferenceMapper;
import io.mango.payment.core.mapper.PaymentEnterpriseSubjectMapper;
import io.mango.payment.core.mapper.PaymentExceptionOrderMapper;
import io.mango.payment.core.mapper.PaymentNotificationRecordMapper;
import io.mango.payment.core.mapper.PaymentOfflineCollectionMapper;
import io.mango.payment.core.mapper.PaymentOperationAuditMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.mapper.PaymentTransactionFlowMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PaymentReadonlyResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentReadonlyResourceService.class);

    private static final Pattern PAYMENT_ORDER_NO_PATTERN = Pattern.compile("^PO\\d{16}$");

    private static final Pattern REFUND_ORDER_NO_PATTERN = Pattern.compile("^RO\\d{16}$");

    private static final Set<String> EXCEPTION_ORDER_HANDLE_ACTIONS = Set.of(
            "ACTIVE_QUERY",
            "ACTIVE_REFUND_QUERY",
            "CLOSE_PAYMENT_ORDER",
            "ADD_EVIDENCE",
            "MANUAL_CLOSE");
    private static final Set<String> PAYMENT_EXCEPTION_TYPES = Set.of(
            PaymentExceptionOrderService.TYPE_DUPLICATE_PAYMENT,
            PaymentExceptionOrderService.TYPE_PAY_TIMEOUT,
            PaymentExceptionOrderService.TYPE_CHANNEL_FAILED);

    private final PaymentApplicationMapper applicationMapper;
    private final PaymentEnterpriseSubjectMapper enterpriseSubjectMapper;
    private final PaymentBusinessOrderMapper businessOrderMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentRefundOrderMapper refundOrderMapper;
    private final PaymentTransactionFlowMapper transactionFlowMapper;
    private final PaymentChannelCapabilityMapper channelCapabilityMapper;
    private final PaymentExceptionOrderMapper exceptionOrderMapper;
    private final PaymentNotificationRecordMapper notificationRecordMapper;
    private final PaymentDifferenceMapper differenceMapper;
    private final PaymentOfflineCollectionMapper offlineCollectionMapper;
    private final PaymentOperationAuditMapper operationAuditMapper;
    private final PaymentOperationAuditService auditService;
    private final PaymentChannelSyncService channelSyncService;
    private final PaymentChannelOrderCloseService channelOrderCloseService;
    private final PaymentOrderStatusFlowService statusFlowService;
    private final PaymentNotificationService notificationService;
    private final PaymentNumberService numberService;

    public PageResult<PaymentBusinessOrderVO> pageBusinessOrders(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String statusCode = PaymentContextSupport.trimToNull(resolved.getStatusCode());
        Long tenantId = PaymentContextSupport.currentTenantId();
        Long applicationId = resolved.getApplicationId();
        Long enterpriseSubjectId = resolved.getEnterpriseSubjectId();
        long total = businessOrderMapper.countBusinessOrders(tenantId, keyword, statusCode, applicationId, enterpriseSubjectId);
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentBusinessOrderVO> rows = businessOrderMapper.selectBusinessOrderPage(
                tenantId, keyword, statusCode, applicationId, enterpriseSubjectId, size, (page - 1) * size);
        rows.forEach(this::fillBusinessOrderStatusName);
        return PageResult.of(rows, total, page, size);
    }

    public PageResult<PaymentChannelCapabilityVO> pageChannelCapabilities(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        Long tenantId = PaymentContextSupport.currentTenantId();
        long total = channelCapabilityMapper.countChannelCapabilities(tenantId, keyword, resolved.getStatus(), resolved.getChannelId());
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentChannelCapabilityVO> rows = channelCapabilityMapper.selectChannelCapabilityPage(
                tenantId, keyword, resolved.getStatus(), resolved.getChannelId(), size, (page - 1) * size);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentBusinessOrderVO detailBusinessOrder(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "业务订单 ID 不能为空");
        PaymentBusinessOrderVO vo = businessOrderMapper.selectBusinessOrderDetail(PaymentContextSupport.currentTenantId(), id);
        Require.notNull(vo, PaymentCode.PAYMENT_READONLY_RESOURCE_NOT_FOUND);
        fillBusinessOrderStatusName(vo);
        vo.setStatusFlows(listStatusFlows(PaymentOrderStatusFlowService.ORDER_TYPE_BUSINESS, id, this::businessStatusName));
        return vo;
    }

    public List<PaymentBusinessOrderStatusVO> listBusinessOrderStatuses() {
        return PaymentBusinessOrderStatusEnum.options().stream().map(status -> {
            PaymentBusinessOrderStatusVO vo = new PaymentBusinessOrderStatusVO();
            vo.setStatusCode(status.getCode());
            vo.setStatusName(status.getLabel());
            return vo;
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentBusinessOrderVO createBusinessOrder(CreatePaymentBusinessOrderCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID.getCode(), "创建业务订单命令不能为空");
        Require.notBlank(command.getAppId(), PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID.getCode(), "AppId 不能为空");
        Require.notBlank(command.getTitle(), PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID.getCode(), "支付标题不能为空");
        Require.notNull(command.getSubjectId(), PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID.getCode(), "收款主体 ID 不能为空");
        Require.notNull(command.getAmount(), PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID.getCode(), "订单金额不能为空");
        Require.isTrue(command.getAmount() > 0, PaymentCode.PAYMENT_AMOUNT_INVALID);
        String appId = command.getAppId().trim();
        LocalDateTime now = LocalDateTime.now();
        String bizOrderNo = PaymentContextSupport.trimToNull(command.getBizOrderNo());
        if (bizOrderNo == null) {
            bizOrderNo = numberService.next(PaymentNumberService.PAY_BIZ_ORDER_NO);
        }
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentApplication application = selectRequiredApplication(tenantId, appId);
        PaymentEnterpriseSubject subject = selectRequiredSubject(tenantId, command.getSubjectId());
        Require.isTrue(businessOrderMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentBusinessOrderEntity>()
                        .eq(PaymentBusinessOrderEntity::getTenantId, tenantId)
                        .eq(PaymentBusinessOrderEntity::getAppCode, application.getAppId())
                        .eq(PaymentBusinessOrderEntity::getBizOrderNo, bizOrderNo)) == null,
                PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID.getCode(), "业务订单号已存在");

        PaymentBusinessOrderEntity entity = new PaymentBusinessOrderEntity();
        entity.setId(IdWorker.getId());
        entity.setBizOrderNo(bizOrderNo);
        entity.setAppCode(application.getAppId());
        entity.setTitle(command.getTitle().trim());
        entity.setSubjectId(subject.getId());
        entity.setAmount(command.getAmount());
        entity.setPaidAmount(0L);
        entity.setRefundedAmount(0L);
        entity.setCurrency(PaymentContextSupport.trimToNull(command.getCurrency()) == null ? "CNY" : command.getCurrency().trim());
        entity.setStatus(PaymentBusinessOrderStatusEnum.TO_PAY.getCode());
        entity.setExpireTime(command.getExpireTime());
        entity.setNotifyUrl(PaymentContextSupport.trimToNull(command.getNotifyUrl()));
        entity.setReturnUrl(PaymentContextSupport.trimToNull(command.getReturnUrl()));
        entity.setExtendInfo(PaymentContextSupport.trimToNull(command.getExtendInfo()));
        entity.setTenantId(tenantId);
        entity.setCreatedBy(PaymentContextSupport.currentUserId());
        entity.setCreatedAt(now);
        entity.setUpdatedBy(PaymentContextSupport.currentUserId());
        entity.setUpdatedAt(now);
        entity.setDelFlag(0);
        businessOrderMapper.insert(entity);
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_BUSINESS,
                entity.getId(),
                entity.getBizOrderNo(),
                null,
                PaymentBusinessOrderStatusEnum.TO_PAY.getCode(),
                "MANUAL_CREATE_BUSINESS_ORDER",
                entity.getBizOrderNo(),
                now,
                "后台创建业务订单");
        return detailBusinessOrder(entity.getId());
    }

    private void fillBusinessOrderStatusName(PaymentBusinessOrderVO vo) {
        if (isExpiredOpenBusinessOrder(vo.getStatus(), vo.getExpireTime())) {
            vo.setStatusName("已过期");
            vo.setPayable(false);
            vo.setPayDisabledReason("订单已过期");
            return;
        }
        vo.setStatusName(PaymentBusinessOrderStatusEnum.labelOf(vo.getStatus()));
        if (PaymentBusinessOrderStatusEnum.TO_PAY.getCode().equals(vo.getStatus())
                || PaymentBusinessOrderStatusEnum.PAYING.getCode().equals(vo.getStatus())) {
            vo.setPayable(vo.getCashierConfigId() != null);
            vo.setPayDisabledReason(vo.getCashierConfigId() == null ? "当前业务订单未匹配收银台配置" : null);
            return;
        }
        vo.setPayable(false);
        vo.setPayDisabledReason("当前状态不可发起支付");
    }

    private PaymentApplication selectRequiredApplication(Long tenantId, String appId) {
        PaymentApplication application = applicationMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentApplication>()
                .eq(PaymentApplication::getTenantId, tenantId)
                .eq(PaymentApplication::getAppId, appId));
        Require.notNull(application, PaymentCode.PAYMENT_APPLICATION_NOT_FOUND);
        Require.isTrue(Integer.valueOf(1).equals(application.getStatus()),
                PaymentCode.PAYMENT_APPLICATION_INVALID.getCode(), "支付应用未启用");
        return application;
    }

    private PaymentEnterpriseSubject selectRequiredSubject(Long tenantId, Long subjectId) {
        PaymentEnterpriseSubject subject = enterpriseSubjectMapper.selectById(subjectId);
        Require.notNull(subject, PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_NOT_FOUND);
        Require.isTrue(tenantId.equals(subject.getTenantId()), PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_NOT_FOUND);
        Require.isTrue(Integer.valueOf(1).equals(subject.getStatus()),
                PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_INVALID.getCode(), "收款主体未启用");
        return subject;
    }

    public PageResult<PaymentOrderVO> pagePaymentOrders(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String statusCode = PaymentContextSupport.trimToNull(resolved.getStatusCode());
        Long tenantId = PaymentContextSupport.currentTenantId();
        long total = paymentOrderMapper.countPaymentOrders(tenantId, keyword, statusCode, resolved.getChannelId());
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentOrderVO> rows = paymentOrderMapper.selectPaymentOrderPage(tenantId, keyword, statusCode, resolved.getChannelId(), size, (page - 1) * size);
        rows.forEach(this::fillPaymentOrderSummary);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentOrderVO detailPaymentOrder(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "支付订单 ID 不能为空");
        PaymentOrderVO vo = paymentOrderMapper.selectPaymentOrderDetail(PaymentContextSupport.currentTenantId(), id);
        Require.notNull(vo, PaymentCode.PAYMENT_READONLY_RESOURCE_NOT_FOUND);
        fillPaymentOrderSummary(vo);
        vo.setStatusFlows(listStatusFlows(PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT, id, this::paymentStatusName));
        return vo;
    }

    public PaymentOrderSyncStatusVO syncPaymentOrderStatus(String payOrderNo) {
        String resolvedPayOrderNo = PaymentContextSupport.trimToNull(payOrderNo);
        Require.notBlank(resolvedPayOrderNo, PaymentCode.PAYMENT_ORDER_NOT_FOUND.getCode(), "支付订单号不能为空");
        PaymentChannelSyncService.PaymentSyncResult queryResult = channelSyncService.syncPaymentStatus(resolvedPayOrderNo);
        auditService.record(
                PaymentOperationAuditService.ACTION_SYNC_PAYMENT_ORDER_STATUS,
                PaymentOperationAuditService.RESOURCE_PAYMENT_ORDER,
                resolvedPayOrderNo,
                PaymentOperationAuditService.RESULT_SUCCESS);
        PaymentOrderSyncStatusVO vo = new PaymentOrderSyncStatusVO();
        vo.setPayOrderNo(queryResult.payOrderNo());
        vo.setStatus(queryResult.status());
        vo.setStatusName(PaymentOrderStatusEnum.labelOf(queryResult.status()));
        vo.setFlowNo(queryResult.flowNo());
        vo.setChanged(queryResult.changed());
        vo.setQueryCount(queryResult.queryCount());
        vo.setLastQueryResult(queryResult.lastQueryResult());
        return vo;
    }

    public List<PaymentOrderStatusVO> listPaymentOrderStatuses() {
        return PaymentOrderStatusEnum.options().stream().map(status -> {
            PaymentOrderStatusVO vo = new PaymentOrderStatusVO();
            vo.setStatusCode(status.getCode());
            vo.setStatusName(status.getLabel());
            return vo;
        }).toList();
    }

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
        rows.forEach(this::fillOfflineCollectionSummary);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentOfflineCollectionVO detailOfflineCollection(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "线下收款 ID 不能为空");
        PaymentOfflineCollectionVO vo = offlineCollectionMapper.selectOfflineCollectionDetail(
                PaymentContextSupport.currentTenantId(), id);
        Require.notNull(vo, PaymentCode.PAYMENT_READONLY_RESOURCE_NOT_FOUND);
        fillOfflineCollectionSummary(vo);
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

    public PageResult<PaymentRefundOrderVO> pageRefundOrders(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String statusCode = PaymentContextSupport.trimToNull(resolved.getStatusCode());
        Long tenantId = PaymentContextSupport.currentTenantId();
        long total = refundOrderMapper.countRefundOrders(tenantId, keyword, statusCode);
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentRefundOrderVO> rows = refundOrderMapper.selectRefundOrderPage(tenantId, keyword, statusCode, size, (page - 1) * size);
        rows.forEach(this::fillRefundOrderSummary);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentRefundOrderVO detailRefundOrder(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "退款订单 ID 不能为空");
        PaymentRefundOrderVO vo = refundOrderMapper.selectRefundOrderDetail(PaymentContextSupport.currentTenantId(), id);
        Require.notNull(vo, PaymentCode.PAYMENT_READONLY_RESOURCE_NOT_FOUND);
        fillRefundOrderSummary(vo);
        vo.setStatusFlows(listStatusFlows(PaymentOrderStatusFlowService.ORDER_TYPE_REFUND, id, this::refundStatusName));
        return vo;
    }

    public List<PaymentRefundOrderStatusVO> listRefundOrderStatuses() {
        return PaymentRefundOrderStatusEnum.options().stream().map(status -> {
            PaymentRefundOrderStatusVO vo = new PaymentRefundOrderStatusVO();
            vo.setStatusCode(status.getCode());
            vo.setStatusName(status.getLabel());
            return vo;
        }).toList();
    }

    public PaymentRefundOrderVO queryRefundOrder(QueryPaymentRefundOrderCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_REFUND_ORDER_INVALID.getCode(), "主动查询退款订单命令不能为空");
        Require.notNull(command.getId(), PaymentCode.PAYMENT_REFUND_ORDER_INVALID.getCode(), "退款订单 ID 不能为空");
        PaymentRefundOrderVO refundOrder = detailRefundOrder(command.getId());
        channelSyncService.syncRefundStatus(refundOrder.getRefundOrderNo());
        return detailRefundOrder(command.getId());
    }

    public PageResult<PaymentTransactionFlowVO> pageTransactionFlows(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        Long tenantId = PaymentContextSupport.currentTenantId();
        long total = transactionFlowMapper.countTransactionFlows(tenantId, keyword);
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentTransactionFlowVO> rows = transactionFlowMapper.selectTransactionFlowPage(tenantId, keyword, size, (page - 1) * size);
        rows.forEach(this::fillTransactionFlowSummary);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentTransactionFlowVO detailTransactionFlow(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(), "交易流水 ID 不能为空");
        PaymentTransactionFlowVO vo = transactionFlowMapper.selectTransactionFlowDetail(PaymentContextSupport.currentTenantId(), id);
        Require.notNull(vo, PaymentCode.PAYMENT_READONLY_RESOURCE_NOT_FOUND);
        fillTransactionFlowSummary(vo);
        return vo;
    }

    public PageResult<PaymentExceptionOrderVO> pageExceptionOrders(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String statusCode = PaymentContextSupport.trimToNull(resolved.getStatusCode());
        Long tenantId = PaymentContextSupport.currentTenantId();
        long total = exceptionOrderMapper.countExceptionOrders(tenantId, keyword, statusCode);
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentExceptionOrderVO> rows = exceptionOrderMapper.selectExceptionOrderPage(tenantId, keyword, statusCode, size, (page - 1) * size);
        rows.forEach(this::fillExceptionOrderSummary);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentExceptionOrderVO detailExceptionOrder(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "异常订单 ID 不能为空");
        PaymentExceptionOrderVO vo = exceptionOrderMapper.selectExceptionOrderDetail(PaymentContextSupport.currentTenantId(), id);
        Require.notNull(vo, PaymentCode.PAYMENT_EXCEPTION_ORDER_NOT_FOUND);
        fillExceptionOrderSummary(vo);
        return vo;
    }

    public List<PaymentExceptionOrderStatusVO> listExceptionOrderStatuses() {
        return List.of(
                exceptionOrderStatus("PENDING"),
                exceptionOrderStatus("PROCESSING"),
                exceptionOrderStatus("HANDLED"),
                exceptionOrderStatus("IGNORED"),
                exceptionOrderStatus("CLOSED"));
    }

    public List<PaymentExceptionOrderActionVO> listExceptionOrderActions() {
        return List.of(
                exceptionOrderAction("ACTIVE_QUERY", "主动查单",
                        List.copyOf(PAYMENT_EXCEPTION_TYPES),
                        "仅适用于关联支付订单的异常，系统会向通道查单并按通道结果推进支付订单状态"),
                exceptionOrderAction("CLOSE_PAYMENT_ORDER", "关闭支付订单",
                        List.copyOf(PAYMENT_EXCEPTION_TYPES),
                        "仅适用于关联支付订单的异常，系统会关闭未支付或支付中的支付订单和业务订单"),
                exceptionOrderAction("ACTIVE_REFUND_QUERY", "主动查退款",
                        List.of(PaymentExceptionOrderService.TYPE_REFUND_MISMATCH),
                        "仅适用于关联退款订单的异常，系统会向通道查退款并按通道结果推进退款订单状态"),
                exceptionOrderAction("ADD_EVIDENCE", "补充凭据",
                        allExceptionTypes(),
                        "记录人工核对材料，异常单进入处理中，不直接修改支付或退款状态"),
                exceptionOrderAction("MANUAL_CLOSE", "人工复核关闭",
                        allExceptionTypes(),
                        "人工确认无需系统动作后关闭异常单，不直接修改支付或退款状态"));
    }

    public PaymentExceptionOrderVO handleExceptionOrder(HandlePaymentExceptionOrderCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID);
        Require.notNull(command.getId(), PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "异常订单 ID 不能为空");
        String handleAction = PaymentContextSupport.trimToNull(command.getHandleAction());
        String handleReason = PaymentContextSupport.trimToNull(command.getHandleReason());
        String handleResult = PaymentContextSupport.trimToNull(command.getHandleResult());
        String handleEvidence = PaymentContextSupport.trimToNull(command.getHandleEvidence());
        Require.notBlank(handleAction, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理动作不能为空");
        Require.notBlank(handleReason, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理原因不能为空");
        Require.notBlank(handleResult, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理结果不能为空");
        Require.isTrue(EXCEPTION_ORDER_HANDLE_ACTIONS.contains(handleAction),
                PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理动作不受支持");
        Require.isTrue(handleAction.length() <= 64, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理动作不能超过 64 个字符");
        Require.isTrue(handleReason.length() <= 512, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理原因不能超过 512 个字符");
        Require.isTrue(handleResult.length() <= 512, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理结果不能超过 512 个字符");
        Require.isTrue(handleEvidence == null || handleEvidence.length() <= 512, PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理凭据不能超过 512 个字符");

        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentExceptionOrderEntity entity = exceptionOrderMapper.selectById(command.getId());
        Require.notNull(entity, PaymentCode.PAYMENT_EXCEPTION_ORDER_NOT_FOUND);
        Require.isTrue(tenantId.equals(entity.getTenantId()) && !Integer.valueOf(1).equals(entity.getDelFlag()),
                PaymentCode.PAYMENT_EXCEPTION_ORDER_NOT_FOUND);
        Require.isTrue(canHandleExceptionOrder(entity.getHandleStatus()),
                PaymentCode.PAYMENT_EXCEPTION_ORDER_HANDLE_STATUS_INVALID);
        Require.isTrue(isActionAllowedForExceptionType(handleAction, entity.getExceptionType()),
                PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理动作不适用于当前异常类型");
        String nextHandleStatus = nextExceptionOrderHandleStatus(handleAction);
        PaymentChannelSyncService.PaymentSyncResult channelQueryResult = activeQueryPayment(handleAction, entity.getRelatedOrderNo());
        if (channelQueryResult != null) {
            nextHandleStatus = "HANDLED";
            handleResult = handleResult + "；查单结果：支付订单 "
                    + channelQueryResult.payOrderNo()
                    + " 当前状态 "
                    + channelQueryResult.status()
                    + (channelQueryResult.changed() ? "，已按通道结果推进" : "，本地状态未变化");
            Require.isTrue(handleResult.length() <= 512,
                    PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理结果不能超过 512 个字符");
        }
        PaymentChannelSyncService.RefundSyncResult refundQueryResult = activeQueryRefund(handleAction, entity.getRelatedOrderNo());
        if (refundQueryResult != null) {
            nextHandleStatus = PaymentRefundOrderStatusEnum.REFUNDING.getCode().equals(normalizeRefundStatus(refundQueryResult.status()))
                    ? "PROCESSING"
                    : "HANDLED";
            handleResult = handleResult + "；查退款结果：退款订单 "
                    + refundQueryResult.refundOrderNo()
                    + " 当前状态 "
                    + refundQueryResult.status()
                    + (refundQueryResult.changed() ? "，已按通道结果推进" : "，本地状态未变化");
            Require.isTrue(handleResult.length() <= 512,
                    PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理结果不能超过 512 个字符");
        }
        PaymentChannelOrderCloseService.CloseResult closeResult = closePaymentOrder(handleAction, entity.getRelatedOrderNo());
        if (closeResult != null) {
            nextHandleStatus = "CLOSED";
            handleResult = handleResult + "；关单结果：支付订单 "
                    + closeResult.payOrderNo()
                    + " 当前状态 "
                    + closeResult.status()
                    + (closeResult.changed() ? "，已关闭" : "，本地状态未变化");
            Require.isTrue(handleResult.length() <= 512,
                    PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "处理结果不能超过 512 个字符");
        }

        LocalDateTime now = LocalDateTime.now();
        Long operatorId = PaymentContextSupport.currentUserId();
        int updated = exceptionOrderMapper.handleExceptionOrder(
                tenantId,
                command.getId(),
                nextHandleStatus,
                handleAction,
                handleReason,
                handleResult,
                handleEvidence,
                operatorId,
                PaymentContextSupport.currentPrincipalName(),
                now);
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_EXCEPTION_ORDER_HANDLE_STATUS_INVALID);

        auditService.record(
                PaymentOperationAuditService.ACTION_HANDLE_EXCEPTION_ORDER,
                PaymentOperationAuditService.RESOURCE_PAYMENT_EXCEPTION_ORDER,
                entity.getExceptionNo(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return detailExceptionOrder(command.getId());
    }

    private PaymentChannelSyncService.PaymentSyncResult activeQueryPayment(String handleAction, String relatedOrderNo) {
        if (!"ACTIVE_QUERY".equals(handleAction)) {
            return null;
        }
        String payOrderNo = PaymentContextSupport.trimToNull(relatedOrderNo);
        Require.isTrue(isPaymentOrderNo(payOrderNo),
                PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "主动查单动作必须关联支付订单号");
        return channelSyncService.syncPaymentStatus(payOrderNo);
    }

    private PaymentChannelSyncService.RefundSyncResult activeQueryRefund(String handleAction, String relatedOrderNo) {
        if (!"ACTIVE_REFUND_QUERY".equals(handleAction)) {
            return null;
        }
        String refundOrderNo = PaymentContextSupport.trimToNull(relatedOrderNo);
        Require.isTrue(isRefundOrderNo(refundOrderNo),
                PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "主动查退款动作必须关联退款订单号");
        return channelSyncService.syncRefundStatus(refundOrderNo);
    }

    private PaymentChannelOrderCloseService.CloseResult closePaymentOrder(String handleAction, String relatedOrderNo) {
        if (!"CLOSE_PAYMENT_ORDER".equals(handleAction)) {
            return null;
        }
        String payOrderNo = PaymentContextSupport.trimToNull(relatedOrderNo);
        Require.isTrue(isPaymentOrderNo(payOrderNo),
                PaymentCode.PAYMENT_EXCEPTION_ORDER_INVALID.getCode(), "关闭支付订单动作必须关联支付订单号");
        return channelOrderCloseService.closePaymentOrder(payOrderNo);
    }

    private String nextExceptionOrderHandleStatus(String handleAction) {
        if ("ADD_EVIDENCE".equals(handleAction)) {
            return "PROCESSING";
        }
        if ("MANUAL_CLOSE".equals(handleAction)) {
            return "CLOSED";
        }
        return "HANDLED";
    }

    private boolean isPaymentOrderNo(String orderNo) {
        return orderNo != null && PAYMENT_ORDER_NO_PATTERN.matcher(orderNo).matches();
    }

    private boolean isRefundOrderNo(String orderNo) {
        return orderNo != null && REFUND_ORDER_NO_PATTERN.matcher(orderNo).matches();
    }

    private boolean isActionAllowedForExceptionType(String handleAction, String exceptionType) {
        if ("ACTIVE_QUERY".equals(handleAction) || "CLOSE_PAYMENT_ORDER".equals(handleAction)) {
            return exceptionType != null && PAYMENT_EXCEPTION_TYPES.contains(exceptionType);
        }
        if ("ACTIVE_REFUND_QUERY".equals(handleAction)) {
            return PaymentExceptionOrderService.TYPE_REFUND_MISMATCH.equals(exceptionType);
        }
        return "ADD_EVIDENCE".equals(handleAction) || "MANUAL_CLOSE".equals(handleAction);
    }

    private List<String> allExceptionTypes() {
        return List.of(
                PaymentExceptionOrderService.TYPE_DUPLICATE_PAYMENT,
                PaymentExceptionOrderService.TYPE_PAY_TIMEOUT,
                PaymentExceptionOrderService.TYPE_CHANNEL_FAILED,
                PaymentExceptionOrderService.TYPE_REFUND_MISMATCH);
    }

    public PageResult<PaymentNotificationRecordVO> pageNotificationRecords(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String statusCode = PaymentContextSupport.trimToNull(resolved.getStatusCode());
        Long tenantId = PaymentContextSupport.currentTenantId();
        long total = notificationRecordMapper.countNotificationRecords(tenantId, keyword, statusCode);
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentNotificationRecordVO> rows = notificationRecordMapper.selectNotificationRecordPage(tenantId, keyword, statusCode, size, (page - 1) * size);
        rows.forEach(this::fillNotificationRecordSummary);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentNotificationRecordVO detailNotificationRecord(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID.getCode(), "通知记录 ID 不能为空");
        PaymentNotificationRecordVO vo = notificationRecordMapper.selectNotificationRecordDetail(PaymentContextSupport.currentTenantId(), id);
        Require.notNull(vo, PaymentCode.PAYMENT_NOTIFICATION_RECORD_NOT_FOUND);
        fillNotificationRecordSummary(vo);
        return vo;
    }

    public List<PaymentNotificationStatusVO> listNotificationStatuses() {
        return List.of(
                notificationStatus("SUCCESS"),
                notificationStatus("RETRYING"),
                notificationStatus("FAILED"),
                notificationStatus("PENDING"));
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentNotificationRecordVO retryNotificationRecord(RetryPaymentNotificationRecordCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID);
        Require.notNull(command.getId(), PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID.getCode(), "通知记录 ID 不能为空");
        String retryReason = PaymentContextSupport.trimToNull(command.getRetryReason());
        Require.notBlank(retryReason, PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID.getCode(), "重推原因不能为空");
        Require.isTrue(retryReason.length() <= 512, PaymentCode.PAYMENT_NOTIFICATION_RECORD_INVALID.getCode(), "重推原因不能超过 512 个字符");

        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentNotificationRecordEntity entity = notificationRecordMapper.selectById(command.getId());
        Require.notNull(entity, PaymentCode.PAYMENT_NOTIFICATION_RECORD_NOT_FOUND);
        Require.isTrue(tenantId.equals(entity.getTenantId()) && !Integer.valueOf(1).equals(entity.getDelFlag()),
                PaymentCode.PAYMENT_NOTIFICATION_RECORD_NOT_FOUND);
        Require.isTrue(canRetryNotificationRecord(entity.getNotifyStatus()),
                PaymentCode.PAYMENT_NOTIFICATION_RECORD_RETRY_STATUS_INVALID);
        Require.notBlank(entity.getPayloadJson(), PaymentCode.PAYMENT_NOTIFICATION_RECORD_RETRY_STATUS_INVALID.getCode(), "通知报文快照不存在，不能人工重推");

        LocalDateTime now = LocalDateTime.now();
        Long operatorId = PaymentContextSupport.currentUserId();
        String retryResult = "人工补偿重推已登记，等待通知任务执行 ACK";
        int updated = notificationRecordMapper.manualRetryNotificationRecord(
                tenantId,
                command.getId(),
                retryReason,
                retryResult,
                now,
                operatorId,
                PaymentContextSupport.currentPrincipalName());
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_NOTIFICATION_RECORD_RETRY_STATUS_INVALID);

        auditService.record(
                PaymentOperationAuditService.ACTION_RETRY_NOTIFICATION_RECORD,
                PaymentOperationAuditService.RESOURCE_PAYMENT_NOTIFICATION_RECORD,
                entity.getNotificationNo(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return detailNotificationRecord(command.getId());
    }

    public int deliverDueNotificationRecords(long limit) {
        int delivered = notificationService.deliverDueNotificationRecords(limit);
        auditService.record(
                PaymentOperationAuditService.ACTION_DELIVER_DUE_NOTIFICATION_RECORDS,
                PaymentOperationAuditService.RESOURCE_PAYMENT_NOTIFICATION_RECORD,
                "DUE_NOTIFICATION_RECORDS",
                PaymentOperationAuditService.RESULT_SUCCESS);
        return delivered;
    }

    public PaymentTaskDispatchResultVO expireOpenPaymentOrders(long limit) {
        long resolvedLimit = validateTaskLimit(limit);
        List<PaymentOrderEntity> orders = paymentOrderMapper.selectExpiredOpenPaymentOrders(
                PaymentContextSupport.currentTenantId(), LocalDateTime.now(), resolvedLimit);
        PaymentTaskDispatchResultVO result = new PaymentTaskDispatchResultVO();
        result.setScannedCount(orders.size());
        for (PaymentOrderEntity order : orders) {
            if (order.getPayOrderNo() == null) {
                result.setSkippedCount(result.getSkippedCount() + 1);
                continue;
            }
            try {
                PaymentChannelOrderCloseService.CloseResult closeResult =
                        channelOrderCloseService.closeExpiredPaymentOrder(order.getPayOrderNo());
                if (closeResult.changed()) {
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } else {
                    result.setSkippedCount(result.getSkippedCount() + 1);
                }
            } catch (RuntimeException ex) {
                result.setFailedCount(result.getFailedCount() + 1);
                LOGGER.warn("Payment expired order close failed: payOrderNo={}", order.getPayOrderNo(), ex);
            }
        }
        auditService.record(
                PaymentOperationAuditService.ACTION_EXPIRE_OPEN_PAYMENT_ORDERS,
                PaymentOperationAuditService.RESOURCE_PAYMENT_ORDER,
                taskResultResourceId("EXPIRE_OPEN_PAYMENT_ORDERS", result),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return result;
    }

    public PaymentTaskDispatchResultVO queryProcessingPaymentOrders(long limit) {
        long resolvedLimit = validateTaskLimit(limit);
        List<PaymentOrderEntity> orders = paymentOrderMapper.selectProcessingPaymentOrders(
                PaymentContextSupport.currentTenantId(), resolvedLimit);
        PaymentTaskDispatchResultVO result = new PaymentTaskDispatchResultVO();
        result.setScannedCount(orders.size());
        for (PaymentOrderEntity order : orders) {
            if (order.getPayOrderNo() == null) {
                result.setSkippedCount(result.getSkippedCount() + 1);
                continue;
            }
            try {
                PaymentChannelSyncService.PaymentSyncResult queryResult =
                        channelSyncService.syncPaymentStatus(order.getPayOrderNo());
                if (queryResult.changed()) {
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } else {
                    result.setSkippedCount(result.getSkippedCount() + 1);
                }
            } catch (RuntimeException ex) {
                result.setFailedCount(result.getFailedCount() + 1);
                LOGGER.warn("Payment processing order query failed: payOrderNo={}", order.getPayOrderNo(), ex);
            }
        }
        auditService.record(
                PaymentOperationAuditService.ACTION_QUERY_PROCESSING_PAYMENT_ORDERS,
                PaymentOperationAuditService.RESOURCE_PAYMENT_ORDER,
                taskResultResourceId("QUERY_PROCESSING_PAYMENT_ORDERS", result),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return result;
    }

    public PageResult<PaymentDifferenceVO> pageDifferences(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String statusCode = PaymentContextSupport.trimToNull(resolved.getStatusCode());
        Long tenantId = PaymentContextSupport.currentTenantId();
        long total = differenceMapper.countDifferences(tenantId, keyword, statusCode);
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentDifferenceVO> rows = differenceMapper.selectDifferencePage(tenantId, keyword, statusCode, size, (page - 1) * size);
        rows.forEach(this::fillDifferenceSummary);
        return PageResult.of(rows, total, page, size);
    }

    public PageResult<PaymentOperationAuditVO> pageOperationAudits(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String operationResult = PaymentContextSupport.trimToNull(resolved.getStatusCode());
        Long tenantId = PaymentContextSupport.currentTenantId();
        long total = operationAuditMapper.countOperationAudits(tenantId, keyword, operationResult);
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentOperationAuditVO> rows = operationAuditMapper.selectOperationAuditPage(
                tenantId, keyword, operationResult, size, (page - 1) * size);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentDifferenceVO detailDifference(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "对账差异 ID 不能为空");
        PaymentDifferenceVO vo = differenceMapper.selectDifferenceDetail(PaymentContextSupport.currentTenantId(), id);
        Require.notNull(vo, PaymentCode.PAYMENT_DIFFERENCE_NOT_FOUND);
        fillDifferenceSummary(vo);
        return vo;
    }

    public List<PaymentDifferenceStatusVO> listDifferenceStatuses() {
        return List.of(
                differenceStatus("PENDING"),
                differenceStatus("PROCESSING"),
                differenceStatus("HANDLED"),
                differenceStatus("IGNORED"),
                differenceStatus("CLOSED"));
    }

    public List<PaymentDifferenceActionVO> listDifferenceActions() {
        return List.of(
                differenceAction("ACTIVE_QUERY", "主动查单"),
                differenceAction("SUPPLEMENT_ORDER", "挂起待认领"),
                differenceAction("IGNORE", "忽略差异"),
                differenceAction("CLOSE", "关闭差异"));
    }

    public PaymentDifferenceVO handleDifference(HandlePaymentDifferenceCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_DIFFERENCE_INVALID);
        Require.notNull(command.getId(), PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "对账差异 ID 不能为空");
        String processAction = PaymentContextSupport.trimToNull(command.getProcessAction());
        String processReason = PaymentContextSupport.trimToNull(command.getProcessReason());
        String processResult = PaymentContextSupport.trimToNull(command.getProcessResult());
        String processEvidence = PaymentContextSupport.trimToNull(command.getProcessEvidence());
        Require.notBlank(processAction, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "处理动作不能为空");
        Require.notBlank(processReason, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "处理原因不能为空");
        Require.notBlank(processResult, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "处理结果不能为空");
        Require.isTrue(isSupportedDifferenceAction(processAction), PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "处理动作不支持");
        Require.isTrue(processAction.length() <= 64, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "处理动作不能超过 64 个字符");
        Require.isTrue(processReason.length() <= 512, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "处理原因不能超过 512 个字符");
        Require.isTrue(processResult.length() <= 512, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "处理结果不能超过 512 个字符");
        Require.isTrue(processEvidence == null || processEvidence.length() <= 512, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "处理凭据不能超过 512 个字符");

        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentDifferenceEntity entity = differenceMapper.selectById(command.getId());
        Require.notNull(entity, PaymentCode.PAYMENT_DIFFERENCE_NOT_FOUND);
        Require.isTrue(tenantId.equals(entity.getTenantId()) && !Integer.valueOf(1).equals(entity.getDelFlag()),
                PaymentCode.PAYMENT_DIFFERENCE_NOT_FOUND);
        Require.isTrue(canHandleDifference(entity.getProcessStatus()),
                PaymentCode.PAYMENT_DIFFERENCE_HANDLE_STATUS_INVALID);
        DifferenceActionResult actionResult = executeDifferenceAction(processAction, entity);
        processResult = mergeProcessResult(processResult, actionResult.message());
        Require.isTrue(processResult.length() <= 512, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "处理结果不能超过 512 个字符");

        LocalDateTime now = LocalDateTime.now();
        Long operatorId = PaymentContextSupport.currentUserId();
        PaymentTransactionFlowEntity adjustFlow = createDifferenceAdjustFlow(now, operatorId, tenantId);
        int updated = differenceMapper.handleDifference(
                tenantId,
                command.getId(),
                actionResult.nextStatus(),
                processAction,
                processReason,
                processResult,
                processEvidence,
                adjustFlow.getId(),
                adjustFlow.getFlowNo(),
                operatorId,
                PaymentContextSupport.currentPrincipalName(),
                now);
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_DIFFERENCE_HANDLE_STATUS_INVALID);

        auditService.record(
                PaymentOperationAuditService.ACTION_HANDLE_DIFFERENCE,
                PaymentOperationAuditService.RESOURCE_PAYMENT_DIFFERENCE,
                entity.getDifferenceNo(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return detailDifference(command.getId());
    }

    private PaymentTransactionFlowEntity createDifferenceAdjustFlow(
            LocalDateTime now,
            Long operatorId,
            Long tenantId) {
        PaymentTransactionFlowEntity flow = new PaymentTransactionFlowEntity();
        flow.setId(IdWorker.getId());
        flow.setFlowNo(numberService.next(PaymentNumberService.PAY_ADJUST_FLOW_NO));
        flow.setBusinessOrderId(null);
        flow.setPaymentOrderId(null);
        flow.setRefundOrderId(null);
        flow.setFlowType("ADJUST_NOTE");
        flow.setAmount(0L);
        flow.setTenantId(tenantId);
        flow.setCreatedBy(operatorId);
        flow.setCreatedAt(now);
        flow.setUpdatedBy(operatorId);
        flow.setUpdatedAt(now);
        transactionFlowMapper.insert(flow);
        return flow;
    }

    private long validateTaskLimit(long limit) {
        Require.isTrue(limit > 0 && limit <= 100, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID.getCode(),
                "任务批次大小必须在 1 到 100 之间");
        return limit;
    }

    private String taskResultResourceId(String taskCode, PaymentTaskDispatchResultVO result) {
        return taskCode
                + ":s=" + result.getScannedCount()
                + ",ok=" + result.getSuccessCount()
                + ",skip=" + result.getSkippedCount()
                + ",fail=" + result.getFailedCount();
    }

    private void fillPaymentOrderSummary(PaymentOrderVO vo) {
        if (isExpiredOpenPaymentOrder(vo.getStatus(), vo.getExpireTime())) {
            vo.setStatusName("已过期");
        } else {
            vo.setStatusName(PaymentOrderStatusEnum.labelOf(vo.getStatus()));
        }
        vo.setFlowNo(paymentOrderMapper.selectLatestFlowNo(PaymentContextSupport.currentTenantId(), vo.getId()));
    }

    private boolean isExpiredOpenBusinessOrder(String status, LocalDateTime expireTime) {
        return expireTime != null
                && !expireTime.isAfter(LocalDateTime.now())
                && (PaymentBusinessOrderStatusEnum.TO_PAY.getCode().equals(status)
                || PaymentBusinessOrderStatusEnum.PAYING.getCode().equals(status));
    }

    private boolean isExpiredOpenPaymentOrder(String status, LocalDateTime expireTime) {
        return expireTime != null
                && !expireTime.isAfter(LocalDateTime.now())
                && (PaymentOrderStatusEnum.CREATED.getCode().equals(status)
                || PaymentOrderStatusEnum.PAYING.getCode().equals(status));
    }

    private void fillRefundOrderSummary(PaymentRefundOrderVO vo) {
        vo.setStatusName(PaymentRefundOrderStatusEnum.labelOf(normalizeRefundStatus(vo.getStatus())));
        vo.setFlowNo(refundOrderMapper.selectLatestFlowNo(PaymentContextSupport.currentTenantId(), vo.getId()));
    }

    private void fillOfflineCollectionSummary(PaymentOfflineCollectionVO vo) {
        vo.setCollectionStatusName(PaymentOfflineCollectionStatusEnum.labelOf(vo.getCollectionStatus()));
    }

    private void fillTransactionFlowSummary(PaymentTransactionFlowVO vo) {
        vo.setFlowTypeName(transactionFlowTypeName(vo.getFlowType()));
    }

    private void fillExceptionOrderSummary(PaymentExceptionOrderVO vo) {
        vo.setExceptionTypeName(exceptionTypeName(vo.getExceptionType()));
        vo.setSeverityName(exceptionSeverityName(vo.getSeverity()));
        vo.setHandleStatusName(exceptionHandleStatusName(vo.getHandleStatus()));
    }

    private void fillNotificationRecordSummary(PaymentNotificationRecordVO vo) {
        vo.setNotificationTypeName(notificationTypeName(vo.getNotificationType()));
        vo.setNotifyStatusName(notificationStatusName(vo.getNotifyStatus()));
    }

    private void fillDifferenceSummary(PaymentDifferenceVO vo) {
        vo.setDifferenceTypeName(differenceTypeName(vo.getDifferenceType()));
        vo.setProcessStatusName(differenceStatusName(vo.getProcessStatus()));
    }

    private boolean canHandleExceptionOrder(String handleStatus) {
        return "PENDING".equals(handleStatus) || "PROCESSING".equals(handleStatus);
    }

    private boolean canRetryNotificationRecord(String notifyStatus) {
        return "FAILED".equals(notifyStatus) || "RETRYING".equals(notifyStatus) || "PENDING".equals(notifyStatus);
    }

    private boolean canHandleDifference(String processStatus) {
        return "PENDING".equals(processStatus) || "PROCESSING".equals(processStatus);
    }

    private boolean isSupportedDifferenceAction(String processAction) {
        return "ACTIVE_QUERY".equals(processAction)
                || "SUPPLEMENT_ORDER".equals(processAction)
                || "IGNORE".equals(processAction)
                || "CLOSE".equals(processAction);
    }

    private DifferenceActionResult executeDifferenceAction(String processAction, PaymentDifferenceEntity entity) {
        if ("ACTIVE_QUERY".equals(processAction)) {
            return executeDifferenceActiveQuery(entity);
        }
        return new DifferenceActionResult(resolveDifferenceNextStatus(processAction), null);
    }

    private DifferenceActionResult executeDifferenceActiveQuery(PaymentDifferenceEntity entity) {
        String relatedOrderNo = PaymentContextSupport.trimToNull(entity.getRelatedOrderNo());
        Require.notBlank(relatedOrderNo, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "主动查单需要关联订单号");
        if (isPaymentOrderNo(relatedOrderNo)) {
            PaymentChannelSyncService.PaymentSyncResult queryResult =
                    channelSyncService.syncPaymentStatus(relatedOrderNo);
            return new DifferenceActionResult(
                    paymentQueryNextStatus(queryResult.status()),
                    "主动查单结果：支付订单 " + queryResult.payOrderNo()
                            + " 当前状态 " + queryResult.status()
                            + (queryResult.changed() ? "，已按通道结果推进" : "，本地状态未变化"));
        }
        if (isRefundOrderNo(relatedOrderNo)) {
            PaymentChannelSyncService.RefundSyncResult queryResult =
                    channelSyncService.syncRefundStatus(relatedOrderNo);
            return new DifferenceActionResult(
                    refundQueryNextStatus(queryResult.status()),
                    "主动查退款结果：退款订单 " + queryResult.refundOrderNo()
                            + " 当前状态 " + queryResult.status()
                            + (queryResult.changed() ? "，已按通道结果推进" : "，本地状态未变化"));
        }
        Require.isTrue(false, PaymentCode.PAYMENT_DIFFERENCE_INVALID.getCode(), "主动查单需要关联本地支付单号或退款单号");
        return new DifferenceActionResult("PROCESSING", null);
    }

    private String paymentQueryNextStatus(String paymentStatus) {
        if (PaymentOrderStatusEnum.PAYING.getCode().equals(paymentStatus)) {
            return "PROCESSING";
        }
        return "HANDLED";
    }

    private String refundQueryNextStatus(String refundStatus) {
        if (PaymentRefundOrderStatusEnum.REFUNDING.getCode().equals(normalizeRefundStatus(refundStatus))) {
            return "PROCESSING";
        }
        return "HANDLED";
    }

    private String mergeProcessResult(String processResult, String actionMessage) {
        if (!StringUtils.hasText(actionMessage)) {
            return processResult;
        }
        return processResult + "；" + actionMessage;
    }

    private String resolveDifferenceNextStatus(String processAction) {
        if ("IGNORE".equals(processAction)) {
            return "IGNORED";
        }
        if ("CLOSE".equals(processAction)) {
            return "CLOSED";
        }
        if ("SUPPLEMENT_ORDER".equals(processAction)) {
            return "PROCESSING";
        }
        return "HANDLED";
    }

    private String differenceTypeName(String differenceType) {
        if ("LOCAL_SUCCESS_CHANNEL_MISSING".equals(differenceType)) {
            return "我方成功通道无单";
        }
        if ("CHANNEL_SUCCESS_LOCAL_MISSING".equals(differenceType)) {
            return "通道成功我方无单";
        }
        if ("AMOUNT_MISMATCH".equals(differenceType)) {
            return "金额不一致";
        }
        if ("STATUS_MISMATCH".equals(differenceType)) {
            return "状态不一致";
        }
        if ("REFUND_MISMATCH".equals(differenceType)) {
            return "退款不一致";
        }
        if ("FEE_MISMATCH".equals(differenceType)) {
            return "手续费不一致";
        }
        return differenceType;
    }

    private String differenceStatusName(String processStatus) {
        if ("PENDING".equals(processStatus)) {
            return "待处理";
        }
        if ("PROCESSING".equals(processStatus)) {
            return "处理中";
        }
        if ("HANDLED".equals(processStatus)) {
            return "已处理";
        }
        if ("IGNORED".equals(processStatus)) {
            return "已忽略";
        }
        if ("CLOSED".equals(processStatus)) {
            return "已关闭";
        }
        return processStatus;
    }

    private String notificationTypeName(String notificationType) {
        if ("PAYMENT_SUCCESS".equals(notificationType)) {
            return "支付成功通知";
        }
        if ("PAYMENT_FAILED".equals(notificationType)) {
            return "支付失败通知";
        }
        if ("PAYMENT_CLOSED".equals(notificationType)) {
            return "支付关闭通知";
        }
        if ("REFUND_SUCCESS".equals(notificationType)) {
            return "退款成功通知";
        }
        if ("REFUND_FAILED".equals(notificationType)) {
            return "退款失败通知";
        }
        return notificationType;
    }

    private String notificationStatusName(String notifyStatus) {
        if ("SUCCESS".equals(notifyStatus)) {
            return "通知成功";
        }
        if ("RETRYING".equals(notifyStatus)) {
            return "重试中";
        }
        if ("FAILED".equals(notifyStatus)) {
            return "通知失败";
        }
        if ("PENDING".equals(notifyStatus)) {
            return "待通知";
        }
        return notifyStatus;
    }

    private String exceptionTypeName(String exceptionType) {
        if ("DUPLICATE_PAYMENT".equals(exceptionType)) {
            return "重复支付";
        }
        if ("PAY_TIMEOUT".equals(exceptionType)) {
            return "超时未回调";
        }
        if ("AMOUNT_MISMATCH".equals(exceptionType)) {
            return "金额不一致";
        }
        if ("STATUS_MISMATCH".equals(exceptionType)) {
            return "状态不一致";
        }
        if ("REFUND_MISMATCH".equals(exceptionType)) {
            return "退款异常";
        }
        if ("CHANNEL_FAILED".equals(exceptionType)) {
            return "通道失败";
        }
        return exceptionType;
    }

    private String exceptionSeverityName(String severity) {
        if ("CRITICAL".equals(severity)) {
            return "严重";
        }
        if ("HIGH".equals(severity)) {
            return "高";
        }
        if ("MEDIUM".equals(severity)) {
            return "中";
        }
        if ("LOW".equals(severity)) {
            return "低";
        }
        return severity;
    }

    private String exceptionHandleStatusName(String handleStatus) {
        if ("PENDING".equals(handleStatus)) {
            return "待处理";
        }
        if ("PROCESSING".equals(handleStatus)) {
            return "处理中";
        }
        if ("HANDLED".equals(handleStatus)) {
            return "已处理";
        }
        if ("IGNORED".equals(handleStatus)) {
            return "已忽略";
        }
        if ("CLOSED".equals(handleStatus)) {
            return "已关闭";
        }
        return handleStatus;
    }

    private PaymentExceptionOrderStatusVO exceptionOrderStatus(String statusCode) {
        PaymentExceptionOrderStatusVO vo = new PaymentExceptionOrderStatusVO();
        vo.setStatusCode(statusCode);
        vo.setStatusName(exceptionHandleStatusName(statusCode));
        return vo;
    }

    private PaymentExceptionOrderActionVO exceptionOrderAction(
            String actionCode,
            String actionName,
            List<String> allowedExceptionTypes,
            String description) {
        PaymentExceptionOrderActionVO vo = new PaymentExceptionOrderActionVO();
        vo.setActionCode(actionCode);
        vo.setActionName(actionName);
        vo.setAllowedExceptionTypes(allowedExceptionTypes);
        vo.setDescription(description);
        return vo;
    }

    private PaymentNotificationStatusVO notificationStatus(String statusCode) {
        PaymentNotificationStatusVO vo = new PaymentNotificationStatusVO();
        vo.setStatusCode(statusCode);
        vo.setStatusName(notificationStatusName(statusCode));
        return vo;
    }

    private PaymentDifferenceStatusVO differenceStatus(String statusCode) {
        PaymentDifferenceStatusVO vo = new PaymentDifferenceStatusVO();
        vo.setStatusCode(statusCode);
        vo.setStatusName(differenceStatusName(statusCode));
        return vo;
    }

    private PaymentDifferenceActionVO differenceAction(String actionCode, String actionName) {
        PaymentDifferenceActionVO vo = new PaymentDifferenceActionVO();
        vo.setActionCode(actionCode);
        vo.setActionName(actionName);
        return vo;
    }

    private String transactionFlowTypeName(String flowType) {
        if ("PAY_SUCCESS".equals(flowType) || "PAYMENT".equals(flowType)) {
            return "支付成功收入";
        }
        if ("REFUND_SUCCESS".equals(flowType) || "REFUND".equals(flowType)) {
            return "退款成功支出";
        }
        if ("PAYMENT_PENDING".equals(flowType)) {
            return "支付待确认备注";
        }
        if ("REFUND_PENDING".equals(flowType)) {
            return "退款待确认备注";
        }
        if ("CHANNEL_FEE".equals(flowType)) {
            return "通道手续费";
        }
        if ("ADJUST_NOTE".equals(flowType)) {
            return "差异处理备注";
        }
        return flowType;
    }

    private String normalizeRefundStatus(String status) {
        return "PROCESSING".equals(status) ? "REFUNDING" : status;
    }

    private List<PaymentOrderStatusFlowVO> listStatusFlows(
            String orderType,
            Long orderId,
            java.util.function.Function<String, String> labelResolver) {
        List<PaymentOrderStatusFlowVO> flows = statusFlowService.list(PaymentContextSupport.currentTenantId(), orderType, orderId);
        flows.forEach(flow -> {
            String statusCode = StringUtils.hasText(flow.getToStatus()) ? flow.getToStatus() : flow.getStatusCode();
            flow.setStatusCode(statusCode);
            flow.setStatusName(labelResolver.apply(statusCode));
            flow.setSource(statusFlowSourceName(flow.getTriggerSource()));
        });
        return flows;
    }

    private String statusFlowSourceName(String triggerSource) {
        if (PaymentOrderStatusFlowService.SOURCE_OPENAPI_CREATE.equals(triggerSource)) {
            return "开放接口创建";
        }
        if (PaymentOrderStatusFlowService.SOURCE_CASHIER_PAY.equals(triggerSource)) {
            return "收银台支付";
        }
        if (PaymentOrderStatusFlowService.SOURCE_CHANNEL_QUERY.equals(triggerSource)) {
            return "主动查单";
        }
        if (PaymentOrderStatusFlowService.SOURCE_CHANNEL_CALLBACK.equals(triggerSource)) {
            return "通道回调";
        }
        if (PaymentOrderStatusFlowService.SOURCE_CHANNEL_CLOSE.equals(triggerSource)) {
            return "受控关单";
        }
        if (PaymentOrderStatusFlowService.SOURCE_OPENAPI_REFUND.equals(triggerSource)) {
            return "开放接口退款";
        }
        if (PaymentOrderStatusFlowService.SOURCE_MANUAL_REFUND_APPROVAL.equals(triggerSource)) {
            return "后台退款审批";
        }
        if (PaymentOrderStatusFlowService.SOURCE_REFUND_QUERY.equals(triggerSource)) {
            return "主动查退款";
        }
        if (PaymentOrderStatusFlowService.SOURCE_RECONCILIATION_COMPENSATE.equals(triggerSource)) {
            return "对账补偿";
        }
        if ("HISTORY_BACKFILL".equals(triggerSource)) {
            return "历史数据初始化";
        }
        return triggerSource;
    }

    private String businessStatusName(String status) {
        return PaymentBusinessOrderStatusEnum.labelOf(status);
    }

    private String paymentStatusName(String status) {
        return PaymentOrderStatusEnum.labelOf(status);
    }

    private String refundStatusName(String status) {
        return PaymentRefundOrderStatusEnum.labelOf(normalizeRefundStatus(status));
    }

    private record DifferenceActionResult(String nextStatus, String message) {
    }

}
