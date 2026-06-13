package io.mango.payment.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.CreatePaymentOpenRefundCommand;
import io.mango.payment.api.command.CreatePaymentRefundApprovalCommand;
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
import io.mango.workflow.api.WorkflowProcessApi;
import io.mango.workflow.api.WorkflowBusinessApplyApi;
import io.mango.workflow.api.command.StartWorkflowProcessCommand;
import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.mango.workflow.api.enums.WorkflowApplyRenderMode;
import io.mango.workflow.api.vo.WorkflowBusinessApplyProgressVO;
import io.mango.workflow.api.vo.WorkflowProcessInstanceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentRefundApprovalService {

    public static final String WORKFLOW_BUSINESS_TYPE = "PAYMENT_REFUND_APPROVAL";
    public static final String WORKFLOW_DEFINITION_KEY = "PAYMENT_REFUND_APPROVAL";
    private static final String WORKFLOW_REVIEWER_NAME = "workflow";

    private final PaymentRefundApprovalMapper refundApprovalMapper;
    private final PaymentApplicationMapper applicationMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentRefundOrderMapper refundOrderMapper;
    private final PaymentRefundApplyService refundApplyService;
    private final PaymentOrderStateService orderStateService;
    private final PaymentOperationAuditService auditService;
    private final PaymentNumberService numberService;
    private final WorkflowProcessApi workflowProcessApi;
    private final WorkflowBusinessApplyApi workflowBusinessApplyApi;
    private final PlatformTransactionManager transactionManager;

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

    public PaymentRefundApprovalVO createRefundApproval(CreatePaymentRefundApprovalCommand command) {
        PaymentRefundApprovalCreateContext context = inTransaction(() -> createRefundApprovalRecord(command));
        WorkflowProcessInstanceVO process;
        try {
            process = startRefundApprovalWorkflow(context.approval(), context.paymentOrder());
        } catch (RuntimeException ex) {
            markWorkflowStartFailedAfterWorkflowException(context, ex);
            throw ex;
        }
        inTransaction(() -> {
            boolean started = updateWorkflowStartProjectionIfPending(
                    context.approval().getTenantId(),
                    context.approval().getApprovalNo(),
                    process);
            if (!started) {
                acceptWorkflowStartProjectionAlreadyProgressed(
                        context.approval().getTenantId(),
                        context.approval().getApprovalNo(),
                        process);
            }
            return null;
        });
        recordCreateRefundApprovalAuditSafely(
                context.approval().getApprovalNo(),
                PaymentOperationAuditService.RESULT_SUCCESS,
                null);
        return detailRefundApproval(context.approval().getId());
    }

    private PaymentRefundApprovalCreateContext createRefundApprovalRecord(CreatePaymentRefundApprovalCommand command) {
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
        PaymentRefundApprovalEntity existingApproval = refundApprovalMapper.selectEntityByBizRefundNoForUpdate(
                tenantId, application.getAppId(), bizRefundNo);
        if (existingApproval != null) {
            return reuseWorkflowStartFailedApproval(command, paymentOrder, existingApproval, now);
        }
        requireRefundAmountAvailable(tenantId, paymentOrder, command.getRefundAmount());

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
        entity.setWorkflowProcessDefinitionKey(WORKFLOW_DEFINITION_KEY);
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
        return new PaymentRefundApprovalCreateContext(entity, paymentOrder);
    }

    private PaymentRefundApprovalCreateContext reuseWorkflowStartFailedApproval(
            CreatePaymentRefundApprovalCommand command,
            PaymentOrderVO paymentOrder,
            PaymentRefundApprovalEntity approval,
            LocalDateTime now) {
        Require.isTrue(PaymentRefundApprovalStatusEnum.WORKFLOW_START_FAILED.getCode().equals(approval.getStatus()),
                PaymentCode.PAYMENT_OPENAPI_IDEMPOTENT_CONFLICT);
        boolean sameRefundTarget = Objects.equals(approval.getPaymentOrderId(), paymentOrder.getId())
                && Objects.equals(approval.getRefundAmount(), command.getRefundAmount());
        Require.isTrue(sameRefundTarget,
                PaymentCode.PAYMENT_OPENAPI_IDEMPOTENT_CONFLICT.getCode(), "退款审批幂等参数不一致");
        requireRefundAmountAvailable(approval.getTenantId(), paymentOrder, command.getRefundAmount());
        approval.setReason(command.getReason().trim());
        approval.setRemark(PaymentContextSupport.trimToNull(command.getRemark()));
        approval.setStatus(PaymentRefundApprovalStatusEnum.PENDING.getCode());
        approval.setRefundOrderId(null);
        approval.setWorkflowApplyId(null);
        approval.setWorkflowProcessInstanceId(null);
        approval.setWorkflowProcessDefinitionKey(WORKFLOW_DEFINITION_KEY);
        approval.setWorkflowApplyStatus(null);
        approval.setWorkflowApplyStatusName(null);
        approval.setWorkflowCurrentTaskNames(null);
        approval.setWorkflowCurrentAssigneeNames(null);
        approval.setWorkflowSyncedAt(null);
        approval.setApplicantId(PaymentContextSupport.currentUserId());
        approval.setApplicantName(PaymentContextSupport.currentPrincipalName());
        approval.setApplyTime(now);
        approval.setReviewerId(null);
        approval.setReviewerName(null);
        approval.setReviewReason(null);
        approval.setReviewTime(null);
        approval.setUpdatedBy(PaymentContextSupport.currentUserId());
        approval.setUpdatedAt(now);
        int updated = refundApprovalMapper.updateById(approval);
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(),
                "退款审批失败记录恢复失败");
        return new PaymentRefundApprovalCreateContext(approval, paymentOrder);
    }

    private void requireRefundAmountAvailable(Long tenantId, PaymentOrderVO paymentOrder, Long refundAmount) {
        long occupyingRefundAmount = normalizedAmount(
                refundOrderMapper.sumOccupyingRefundAmount(tenantId, paymentOrder.getId()));
        long pendingApprovalAmount = normalizedAmount(
                refundApprovalMapper.sumPendingApprovalAmount(tenantId, paymentOrder.getId()));
        orderStateService.requireRefundAmount(refundAmount, paymentOrder.getAmount(), occupyingRefundAmount + pendingApprovalAmount);
    }

    public void syncWorkflowProjection(Long tenantId, String approvalNo) {
        Require.notNull(tenantId, PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "租户 ID 不能为空");
        Require.notBlank(approvalNo, PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "退款审批单号不能为空");
        R<WorkflowBusinessApplyProgressVO> response = workflowBusinessApplyApi.latestProgress(WORKFLOW_BUSINESS_TYPE, approvalNo);
        Require.isTrue(response != null && response.isSuccess(), PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(),
                response == null ? "查询工作流审批进度失败" : response.getMsg());
        WorkflowBusinessApplyProgressVO progress = response.getData();
        if (progress == null) {
            return;
        }
        inTransaction(() -> {
            updateWorkflowProgressProjection(tenantId, approvalNo, progress);
            return null;
        });
    }

    public void approveByWorkflow(Long tenantId, String approvalNo, String processInstanceId) {
        Require.notNull(tenantId, PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "租户 ID 不能为空");
        Require.notBlank(approvalNo, PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "退款审批单号不能为空");
        PaymentRefundApprovalEntity approval = inTransaction(() -> markApprovalWorkflowApproved(tenantId, approvalNo, processInstanceId));
        if (approval == null) {
            return;
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
        inTransaction(() -> {
            PaymentRefundApprovalEntity locked = refundApprovalMapper.selectEntityByApprovalNoForUpdate(tenantId, approvalNo);
            Require.notNull(locked, PaymentCode.PAYMENT_REFUND_APPROVAL_NOT_FOUND);
            locked.setRefundOrderId(refundOrder.getId());
            locked.setStatus(PaymentRefundApprovalStatusEnum.APPROVED.getCode());
            locked.setUpdatedBy(PaymentContextSupport.currentUserId());
            locked.setUpdatedAt(LocalDateTime.now());
            refundApprovalMapper.updateById(locked);
            auditService.record(
                    PaymentOperationAuditService.ACTION_APPROVE_REFUND_APPROVAL,
                    PaymentOperationAuditService.RESOURCE_PAYMENT_REFUND_APPROVAL,
                    approvalNo,
                    PaymentOperationAuditService.RESULT_SUCCESS);
            return null;
        });
    }

    private PaymentRefundApprovalEntity markApprovalWorkflowApproved(Long tenantId, String approvalNo, String processInstanceId) {
        PaymentRefundApprovalEntity approval = refundApprovalMapper.selectEntityByApprovalNoForUpdate(tenantId, approvalNo);
        Require.notNull(approval, PaymentCode.PAYMENT_REFUND_APPROVAL_NOT_FOUND);
        if (PaymentRefundApprovalStatusEnum.APPROVED.getCode().equals(approval.getStatus())) {
            return null;
        }
        Require.isTrue(PaymentRefundApprovalStatusEnum.IN_APPROVAL.getCode().equals(approval.getStatus())
                        || PaymentRefundApprovalStatusEnum.PENDING.getCode().equals(approval.getStatus()),
                PaymentCode.PAYMENT_REFUND_APPROVAL_STATUS_INVALID);
        LocalDateTime now = LocalDateTime.now();
        approval.setReviewerId(null);
        approval.setReviewerName(WORKFLOW_REVIEWER_NAME);
        approval.setReviewReason("工作流审批通过");
        approval.setReviewTime(now);
        approval.setUpdatedBy(PaymentContextSupport.currentUserId());
        approval.setUpdatedAt(now);
        approval.setWorkflowProcessInstanceId(PaymentContextSupport.trimToNull(processInstanceId));
        approval.setWorkflowApplyStatus(WorkflowApplyStatus.APPROVED.name());
        approval.setWorkflowApplyStatusName(WorkflowApplyStatus.APPROVED.getLabel());
        approval.setWorkflowCurrentTaskNames(null);
        approval.setWorkflowCurrentAssigneeNames(null);
        approval.setWorkflowSyncedAt(now);
        refundApprovalMapper.updateById(approval);
        return approval;
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectByWorkflow(Long tenantId, String approvalNo, String processInstanceId, String reason) {
        Require.notNull(tenantId, PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "租户 ID 不能为空");
        Require.notBlank(approvalNo, PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "退款审批单号不能为空");
        PaymentRefundApprovalEntity approval = refundApprovalMapper.selectEntityByApprovalNoForUpdate(tenantId, approvalNo);
        Require.notNull(approval, PaymentCode.PAYMENT_REFUND_APPROVAL_NOT_FOUND);
        if (PaymentRefundApprovalStatusEnum.REJECTED.getCode().equals(approval.getStatus())) {
            return;
        }
        Require.isTrue(PaymentRefundApprovalStatusEnum.IN_APPROVAL.getCode().equals(approval.getStatus())
                        || PaymentRefundApprovalStatusEnum.PENDING.getCode().equals(approval.getStatus()),
                PaymentCode.PAYMENT_REFUND_APPROVAL_STATUS_INVALID);
        LocalDateTime now = LocalDateTime.now();
        approval.setStatus(PaymentRefundApprovalStatusEnum.REJECTED.getCode());
        approval.setReviewerId(null);
        approval.setReviewerName(WORKFLOW_REVIEWER_NAME);
        approval.setReviewReason(PaymentContextSupport.trimToNull(reason) == null ? "工作流审批拒绝" : reason.trim());
        approval.setReviewTime(now);
        approval.setWorkflowProcessInstanceId(PaymentContextSupport.trimToNull(processInstanceId));
        approval.setWorkflowApplyStatus(WorkflowApplyStatus.REJECTED.name());
        approval.setWorkflowApplyStatusName(WorkflowApplyStatus.REJECTED.getLabel());
        approval.setWorkflowCurrentTaskNames(null);
        approval.setWorkflowCurrentAssigneeNames(null);
        approval.setWorkflowSyncedAt(now);
        approval.setUpdatedBy(PaymentContextSupport.currentUserId());
        approval.setUpdatedAt(now);
        refundApprovalMapper.updateById(approval);
        auditService.record(
                PaymentOperationAuditService.ACTION_REJECT_REFUND_APPROVAL,
                PaymentOperationAuditService.RESOURCE_PAYMENT_REFUND_APPROVAL,
                approval.getApprovalNo(),
                PaymentOperationAuditService.RESULT_REJECTED);
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

    private WorkflowProcessInstanceVO startRefundApprovalWorkflow(PaymentRefundApprovalEntity approval, PaymentOrderVO paymentOrder) {
        StartWorkflowProcessCommand command = new StartWorkflowProcessCommand();
        command.setDefinitionKey(WORKFLOW_DEFINITION_KEY);
        command.setBusinessType(WORKFLOW_BUSINESS_TYPE);
        command.setBusinessKey(approval.getApprovalNo());
        command.setApplyId(approval.getId());
        command.setRenderMode(WorkflowApplyRenderMode.CUSTOM_PAGE);
        command.setApplyPageKey("payment.refundApproval.apply");
        command.setApprovePageKey("payment.refundApproval.approve");
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("tenantId", approval.getTenantId());
        variables.put("approvalId", approval.getId());
        variables.put("approvalNo", approval.getApprovalNo());
        variables.put("paymentOrderId", approval.getPaymentOrderId());
        variables.put("payOrderNo", paymentOrder.getPayOrderNo());
        variables.put("bizOrderNo", approval.getBizOrderNo());
        variables.put("bizRefundNo", approval.getBizRefundNo());
        variables.put("refundAmount", approval.getRefundAmount());
        variables.put("appId", approval.getAppId());
        variables.put("businessOrderId", approval.getBusinessOrderId());
        command.setVariables(variables);
        R<WorkflowProcessInstanceVO> response = workflowProcessApi.start(command);
        Require.isTrue(response != null && response.isSuccess(), PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(),
                response == null ? "发起退款审批工作流失败" : response.getMsg());
        WorkflowProcessInstanceVO process = response.getData();
        Require.notNull(process, PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "发起退款审批工作流未返回流程实例");
        Require.notBlank(process.getProcessInstanceId(), PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(),
                "发起退款审批工作流未返回流程实例 ID");
        return process;
    }

    private boolean updateWorkflowStartProjectionIfPending(Long tenantId, String approvalNo, WorkflowProcessInstanceVO process) {
        int updated = refundApprovalMapper.update(null, new UpdateWrapper<PaymentRefundApprovalEntity>()
                .eq("tenant_id", tenantId)
                .eq("approval_no", approvalNo)
                .eq("del_flag", 0)
                .eq("status", PaymentRefundApprovalStatusEnum.PENDING.getCode())
                .set("status", PaymentRefundApprovalStatusEnum.IN_APPROVAL.getCode())
                .set("workflow_process_instance_id", process.getProcessInstanceId())
                .set("workflow_apply_id", process.getApplyId())
                .set("workflow_process_definition_key", WORKFLOW_DEFINITION_KEY)
                .set("workflow_current_task_names",
                        PaymentContextSupport.trimToNull(process.getCurrentTaskName()))
                .set("workflow_synced_at", LocalDateTime.now()));
        return updated == 1;
    }

    private void acceptWorkflowStartProjectionAlreadyProgressed(
            Long tenantId,
            String approvalNo,
            WorkflowProcessInstanceVO process) {
        PaymentRefundApprovalEntity locked = refundApprovalMapper.selectEntityByApprovalNoForUpdate(tenantId, approvalNo);
        Require.notNull(locked, PaymentCode.PAYMENT_REFUND_APPROVAL_NOT_FOUND);
        Require.isTrue(isWorkflowStartProjectionAcceptable(locked.getStatus()),
                PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(), "退款审批工作流启动状态更新失败");
        LocalDateTime now = LocalDateTime.now();
        locked.setWorkflowProcessInstanceId(process.getProcessInstanceId());
        locked.setWorkflowApplyId(process.getApplyId());
        locked.setWorkflowProcessDefinitionKey(WORKFLOW_DEFINITION_KEY);
        if (PaymentRefundApprovalStatusEnum.IN_APPROVAL.getCode().equals(locked.getStatus())) {
            locked.setWorkflowCurrentTaskNames(PaymentContextSupport.trimToNull(process.getCurrentTaskName()));
        }
        locked.setWorkflowSyncedAt(now);
        locked.setUpdatedBy(PaymentContextSupport.currentUserId());
        locked.setUpdatedAt(now);
        int updated = refundApprovalMapper.updateById(locked);
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(),
                "退款审批工作流启动投影同步失败");
    }

    private boolean isWorkflowStartProjectionAcceptable(String status) {
        return PaymentRefundApprovalStatusEnum.IN_APPROVAL.getCode().equals(status)
                || PaymentRefundApprovalStatusEnum.APPROVED.getCode().equals(status)
                || PaymentRefundApprovalStatusEnum.REJECTED.getCode().equals(status);
    }

    private void markWorkflowStartFailedAfterWorkflowException(
            PaymentRefundApprovalCreateContext context,
            RuntimeException workflowException) {
        try {
            inTransaction(() -> {
                markWorkflowStartFailed(context.approval().getTenantId(),
                        context.approval().getApprovalNo(),
                        workflowException.getMessage());
                return null;
            });
        } catch (RuntimeException compensationException) {
            workflowException.addSuppressed(compensationException);
            return;
        }
        recordCreateRefundApprovalAuditSafely(
                context.approval().getApprovalNo(),
                PaymentOperationAuditService.RESULT_REJECTED,
                workflowException);
    }

    private void recordCreateRefundApprovalAuditSafely(
            String approvalNo,
            String result,
            RuntimeException ownerException) {
        try {
            auditService.record(
                    PaymentOperationAuditService.ACTION_CREATE_REFUND_APPROVAL,
                    PaymentOperationAuditService.RESOURCE_PAYMENT_REFUND_APPROVAL,
                    approvalNo,
                    result);
        } catch (RuntimeException auditException) {
            if (ownerException != null) {
                ownerException.addSuppressed(auditException);
            }
            log.warn("Record refund approval workflow start audit failed, approvalNo={}, result={}",
                    approvalNo, result, auditException);
        }
    }

    private void markWorkflowStartFailed(Long tenantId, String approvalNo, String reason) {
        int updated = refundApprovalMapper.update(null, new UpdateWrapper<PaymentRefundApprovalEntity>()
                .eq("tenant_id", tenantId)
                .eq("approval_no", approvalNo)
                .eq("del_flag", 0)
                .eq("status", PaymentRefundApprovalStatusEnum.PENDING.getCode())
                .set("status", PaymentRefundApprovalStatusEnum.WORKFLOW_START_FAILED.getCode())
                .set("reviewer_id", PaymentContextSupport.currentUserId())
                .set("reviewer_name", PaymentContextSupport.currentPrincipalName())
                .set("review_reason", PaymentContextSupport.trimToNull(reason) == null
                        ? "退款审批工作流启动失败"
                        : reason)
                .set("review_time", LocalDateTime.now())
                .set("updated_by", PaymentContextSupport.currentUserId())
                .set("updated_at", LocalDateTime.now()));
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_REFUND_APPROVAL_INVALID.getCode(),
                "退款审批工作流启动失败状态更新失败");
    }

    private void updateWorkflowProgressProjection(
            Long tenantId,
            String approvalNo,
            WorkflowBusinessApplyProgressVO progress) {
        refundApprovalMapper.update(null, new UpdateWrapper<PaymentRefundApprovalEntity>()
                .eq("tenant_id", tenantId)
                .eq("approval_no", approvalNo)
                .eq("del_flag", 0)
                .set("workflow_apply_id", progress.getApplyId())
                .set("workflow_process_instance_id",
                        PaymentContextSupport.trimToNull(progress.getProcessInstanceId()))
                .set("workflow_apply_status",
                        progress.getApplyStatus() == null ? null : progress.getApplyStatus().name())
                .set("workflow_apply_status_name",
                        PaymentContextSupport.trimToNull(progress.getApplyStatusName()))
                .set("workflow_current_task_names",
                        PaymentContextSupport.trimToNull(progress.getCurrentTaskNames()))
                .set("workflow_current_assignee_names",
                        PaymentContextSupport.trimToNull(progress.getCurrentAssigneeNames()))
                .set("workflow_synced_at", LocalDateTime.now()));
    }

    private <T> T inTransaction(java.util.function.Supplier<T> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template.execute(status -> action.get());
    }

    private record PaymentRefundApprovalCreateContext(
            PaymentRefundApprovalEntity approval,
            PaymentOrderVO paymentOrder) {
    }

}
