package io.mango.workflow.core.service.impl;

import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.workflow.api.WorkflowBusinessApplyAccessContext;
import io.mango.workflow.api.WorkflowBusinessApplyDataPermissionProvider;
import io.mango.workflow.api.enums.WorkflowCode;
import io.mango.workflow.core.entity.WorkflowBusinessApplyEntity;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 统一执行业务申请读取权限校验。
 * <p>
 * 业务模块通过 Provider 在自己的业务表上完成细粒度判断；没有匹配 Provider
 * 时仅允许当前租户内的申请人读取，避免历史接口在未接入 Provider 时退化成全局可读。
 */
@Component
public class WorkflowBusinessApplyAccessChecker {

    private final ObjectProvider<WorkflowBusinessApplyDataPermissionProvider> providers;

    public WorkflowBusinessApplyAccessChecker(
            ObjectProvider<WorkflowBusinessApplyDataPermissionProvider> providers) {
        this.providers = providers;
    }

    /** 校验用户态读取；失败时抛出稳定的业务错误码。 */
    public void check(WorkflowBusinessApplyEntity apply) {
        Require.notNull(apply, WorkflowCode.APPLY_NOT_FOUND);
        Require.isTrue(isAllowed(apply), WorkflowCode.APPLY_ACCESS_DENIED);
    }

    /** 返回用户态读取是否被允许，不改变异常边界，供批量查询过滤使用。 */
    public boolean isAllowed(WorkflowBusinessApplyEntity apply) {
        if (apply == null) {
            return false;
        }
        WorkflowBusinessApplyAccessContext context = contextOf(apply);
        String currentTenant = normalize(MangoContextHolder.tenantId());
        if (!StringUtils.hasText(currentTenant) || !StringUtils.hasText(context.tenantId())
                || !currentTenant.equals(context.tenantId())) {
            return false;
        }
        String businessType = context.businessType();
        List<WorkflowBusinessApplyDataPermissionProvider> matched = providers.orderedStream()
                .filter(provider -> supports(provider, businessType))
                .toList();
        boolean allowed = matched.isEmpty()
                ? defaultOwnerTenantCheck(context)
                : matched.stream().anyMatch(provider -> provider.canRead(context));
        return allowed;
    }

    /** 从 Workflow 持久化事实构造不可伪造的权限上下文。 */
    public WorkflowBusinessApplyAccessContext contextOf(WorkflowBusinessApplyEntity apply) {
        return new WorkflowBusinessApplyAccessContext(
                apply.getId(),
                apply.getProcessInstanceId(),
                normalize(apply.getBusinessType()),
                normalize(apply.getBusinessKey()),
                normalize(apply.getTenantId()),
                apply.getOrgId(),
                apply.getApplicantId());
    }

    private boolean supports(WorkflowBusinessApplyDataPermissionProvider provider, String businessType) {
        return provider != null && provider.supports(businessType);
    }

    private boolean defaultOwnerTenantCheck(WorkflowBusinessApplyAccessContext context) {
        Long currentUser = MangoContextHolder.userId();
        if (currentUser == null) {
            return false;
        }
        return currentUser.equals(context.applicantId());
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
