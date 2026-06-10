package io.mango.payment.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.CreatePaymentOpenRefundCommand;
import io.mango.payment.api.command.CreatePaymentRefundApprovalCommand;
import io.mango.payment.api.command.ReviewPaymentRefundApprovalCommand;
import io.mango.payment.api.enums.PaymentRefundApprovalStatusEnum;
import io.mango.payment.api.vo.PaymentOpenRefundOrderVO;
import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.api.vo.PaymentRefundApprovalStatusVO;
import io.mango.payment.api.vo.PaymentRefundApprovalVO;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.core.entity.PaymentApplication;
import io.mango.payment.core.entity.PaymentRefundApprovalEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundApprovalMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentRefundApprovalService {

    private static final String APPROVE_ACTION = "APPROVE";
    private static final String REJECT_ACTION = "REJECT";

    private final PaymentRefundApprovalMapper refundApprovalMapper;
    private final PaymentApplicationMapper applicationMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentRefundOrderMapper refundOrderMapper;
    private final PaymentRefundApplyService refundApplyService;
    private final PaymentOrderStateService orderStateService;
    private final PaymentOperationAuditService auditService;
    private final PaymentNumberService numberService;

    public PageResult<PaymentRefundApprovalVO> pageRefundApprovals(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String statusCode = PaymentContextSupport.trimToNull(resolved.getStatusCode());
        Long tenantId = PaymentContextSupport.currentTenantId();
        long total = refundApprovalMapper.countRefundApprovals(tenantId, keyword, statusCode);
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentRefundApprovalVO> rows = refundApprovalMapper.selectRefundApprovalPage(
                tenantId, keyword, statusCode, size, (page - 1) * size);
        rows.forEach(this::fillStatusName);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentRefundApprovalVO detailRefundApproval(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "退款审批 ID 不能为空");
        PaymentRefundApprovalVO vo = refundApprovalMapper.selectRefundApprovalDetail(PaymentContextSupport.currentTenantId(), id);
        Require.notNull(vo, PaymentCode.PAYMENT_REFUND_APPROVAL_NOT_FOUND);
        fillStatusName(vo);
        return vo;
    }

    public List<PaymentRefundApprovalStatusVO> listRefundApprovalStatuses() {
        return PaymentRefundApprovalStatusEnum.options().stream().map(status -> {
            PaymentRefundApprovalStatusVO vo = new PaymentRefundApprovalStatusVO();
            vo.setStatusCode(status.getCode());
            vo.setStatusName(status.getLabel());
            return vo;
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentRefundApprovalVO createRefundApproval(CreatePaymentRefundApprovalCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "创建退款审批命令不能为空");
        Require.notNull(command.getPaymentOrderId(), PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "原支付订单 ID 不能为空");
        Require.notNull(command.getRefundAmount(), PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "退款金额不能为空");
        Require.isTrue(command.getRefundAmount() > 0, PaymentCode.PAYMENT_AMOUNT_INVALID);
        Require.notBlank(command.getReason(), PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "退款原因不能为空");

        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentOrderVO paymentOrder = paymentOrderMapper.selectPaymentOrderById(tenantId, command.getPaymentOrderId());
        Require.notNull(paymentOrder, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        Require.isTrue("SUCCESS".equals(paymentOrder.getStatus()) && Integer.valueOf(1).equals(paymentOrder.getSuccessFlag()),
                PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "只有有效成功支付订单允许申请后台退款");
        PaymentApplication application = selectRequiredApplication(tenantId, paymentOrder.getAppId());
        LocalDateTime now = LocalDateTime.now();
        String bizRefundNo = PaymentContextSupport.trimToNull(command.getBizRefundNo());
        if (bizRefundNo == null) {
            bizRefundNo = numberService.next(PaymentNumberService.PAY_BIZ_REFUND_NO);
        }
        Require.isTrue(refundOrderMapper.selectOpenRefundOrder(tenantId, application.getAppId(), bizRefundNo) == null,
                PaymentCode.PAYMENT_OPENAPI_IDEMPOTENT_CONFLICT);
        long occupyingRefundAmount = normalizedAmount(refundOrderMapper.sumOccupyingRefundAmount(tenantId, paymentOrder.getId()));
        orderStateService.requireRefundAmount(command.getRefundAmount(), paymentOrder.getAmount(), occupyingRefundAmount);

        PaymentRefundApprovalEntity entity = new PaymentRefundApprovalEntity();
        entity.setApprovalNo(numberService.next(PaymentNumberService.PAY_REFUND_APPROVAL_NO));
        entity.setBusinessOrderId(paymentOrder.getBusinessOrderId());
        entity.setPaymentOrderId(paymentOrder.getId());
        entity.setBizOrderNo(paymentOrder.getBizOrderNo());
        entity.setBizRefundNo(bizRefundNo);
        entity.setAppId(application.getAppId());
        entity.setRefundAmount(command.getRefundAmount());
        entity.setReason(command.getReason().trim());
        entity.setRemark(PaymentContextSupport.trimToNull(command.getRemark()));
        entity.setStatus(PaymentRefundApprovalStatusEnum.PENDING.getCode());
        entity.setApplicantId(PaymentContextSupport.currentUserId());
        entity.setApplicantName(PaymentContextSupport.currentPrincipalName());
        entity.setApplyTime(now);
        entity.setTenantId(tenantId);
        entity.setCreatedBy(PaymentContextSupport.currentUserId());
        entity.setCreatedAt(now);
        entity.setUpdatedBy(PaymentContextSupport.currentUserId());
        entity.setUpdatedAt(now);
        entity.setDelFlag(0);
        refundApprovalMapper.insert(entity);
        auditService.record(
                PaymentOperationAuditService.ACTION_CREATE_REFUND_APPROVAL,
                PaymentOperationAuditService.RESOURCE_PAYMENT_REFUND_APPROVAL,
                entity.getApprovalNo(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return detailRefundApproval(entity.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentRefundApprovalVO reviewRefundApproval(ReviewPaymentRefundApprovalCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "审核退款审批命令不能为空");
        Require.notNull(command.getId(), PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "退款审批 ID 不能为空");
        Require.notBlank(command.getAction(), PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "审核动作不能为空");
        Require.notBlank(command.getReviewReason(), PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "审核说明不能为空");
        String action = command.getAction().trim();
        Require.isTrue(APPROVE_ACTION.equals(action) || REJECT_ACTION.equals(action),
                PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "审核动作只能为 APPROVE 或 REJECT");

        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentRefundApprovalEntity approval = refundApprovalMapper.selectEntityForUpdate(tenantId, command.getId());
        Require.notNull(approval, PaymentCode.PAYMENT_REFUND_APPROVAL_NOT_FOUND);
        Require.isTrue(PaymentRefundApprovalStatusEnum.PENDING.getCode().equals(approval.getStatus()),
                PaymentCode.PAYMENT_REFUND_APPROVAL_STATUS_INVALID);
        Require.isTrue(!PaymentContextSupport.currentUserId().equals(approval.getApplicantId()),
                PaymentCode.PAYMENT_REFUND_APPROVAL_STATUS_INVALID.getCode(), "退款审批申请人不能审核自己的申请");

        LocalDateTime now = LocalDateTime.now();
        approval.setReviewerId(PaymentContextSupport.currentUserId());
        approval.setReviewerName(PaymentContextSupport.currentPrincipalName());
        approval.setReviewReason(command.getReviewReason().trim());
        approval.setReviewTime(now);
        approval.setUpdatedBy(PaymentContextSupport.currentUserId());
        approval.setUpdatedAt(now);
        if (REJECT_ACTION.equals(action)) {
            approval.setStatus(PaymentRefundApprovalStatusEnum.REJECTED.getCode());
            refundApprovalMapper.updateById(approval);
            auditService.record(
                    PaymentOperationAuditService.ACTION_REJECT_REFUND_APPROVAL,
                    PaymentOperationAuditService.RESOURCE_PAYMENT_REFUND_APPROVAL,
                    approval.getApprovalNo(),
                    PaymentOperationAuditService.RESULT_REJECTED);
            return detailRefundApproval(approval.getId());
        }

        PaymentApplication application = selectRequiredApplication(tenantId, approval.getAppId());
        CreatePaymentOpenRefundCommand refundCommand = new CreatePaymentOpenRefundCommand();
        refundCommand.setTenantId(tenantId);
        refundCommand.setAppId(application.getAppId());
        refundCommand.setBizOrderNo(approval.getBizOrderNo());
        refundCommand.setBizRefundNo(approval.getBizRefundNo());
        refundCommand.setRefundAmount(approval.getRefundAmount());
        refundCommand.setReason(approval.getReason());
        PaymentOpenRefundOrderVO refundOrder = refundApplyService.applyRefund(
                application,
                refundCommand,
                PaymentOrderStatusFlowService.SOURCE_MANUAL_REFUND_APPROVAL,
                approval.getApprovalNo(),
                true);
        approval.setRefundOrderId(refundOrder.getId());
        approval.setStatus(PaymentRefundApprovalStatusEnum.APPROVED.getCode());
        refundApprovalMapper.updateById(approval);
        auditService.record(
                PaymentOperationAuditService.ACTION_APPROVE_REFUND_APPROVAL,
                PaymentOperationAuditService.RESOURCE_PAYMENT_REFUND_APPROVAL,
                approval.getApprovalNo(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return detailRefundApproval(approval.getId());
    }

    private PaymentApplication selectRequiredApplication(Long tenantId, String appId) {
        PaymentApplication application = applicationMapper.selectOne(new LambdaQueryWrapper<PaymentApplication>()
                .eq(PaymentApplication::getTenantId, tenantId)
                .eq(PaymentApplication::getAppId, appId));
        Require.notNull(application, PaymentCode.PAYMENT_APPLICATION_NOT_FOUND);
        Require.isTrue(Integer.valueOf(1).equals(application.getStatus()),
                PaymentCode.PAYMENT_APPLICATION_INVALID.getCode(), "支付应用未启用");
        return application;
    }

    private void fillStatusName(PaymentRefundApprovalVO vo) {
        vo.setStatusName(PaymentRefundApprovalStatusEnum.labelOf(vo.getStatus()));
    }

    private long normalizedAmount(Long amount) {
        return amount == null ? 0L : amount;
    }

}
