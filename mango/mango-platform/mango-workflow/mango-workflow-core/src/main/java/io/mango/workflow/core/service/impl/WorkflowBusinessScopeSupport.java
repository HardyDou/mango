package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.workflow.api.enums.WorkflowCode;
import io.mango.workflow.core.entity.WorkflowBusinessApplyEntity;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyMapper;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 业务申请收窄口径的共享实现。
 *
 * <p>流转记录与活动时间轴共用同一套「租户 + 业务类型 + 业务键 + 申请部门」收窄规则与参数校验，
 * 因此把这段无状态逻辑收在一处，避免两份实现各自漂移。它不是 Spring bean：
 * 调用方把自己持有的 {@link WorkflowBusinessApplyMapper} 传进来即可。
 */
public final class WorkflowBusinessScopeSupport {

    private WorkflowBusinessScopeSupport() {
    }

    /** 按租户、业务类型、业务键与申请部门收窄命中的流程实例 ID，保持去重与稳定顺序。 */
    public static List<String> processInstanceIds(WorkflowBusinessApplyMapper mapper, Long tenantId,
                                                 String businessType, String businessKey, List<Long> deptIds) {
        return mapper.selectList(new LambdaQueryWrapper<WorkflowBusinessApplyEntity>()
                        .select(WorkflowBusinessApplyEntity::getProcessInstanceId)
                        .eq(WorkflowBusinessApplyEntity::getTenantId, String.valueOf(tenantId))
                        .eq(WorkflowBusinessApplyEntity::getBusinessType, businessType)
                        .eq(WorkflowBusinessApplyEntity::getBusinessKey, businessKey)
                        .in(!deptIds.isEmpty(), WorkflowBusinessApplyEntity::getApplicantDeptId, deptIds)
                        .isNotNull(WorkflowBusinessApplyEntity::getProcessInstanceId))
                .stream()
                .map(WorkflowBusinessApplyEntity::getProcessInstanceId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    /** 时间区间校验：两端都可空，同时给定开始晚于结束时拒绝。 */
    public static void validateTimeRange(LocalDateTime begin, LocalDateTime end) {
        Require.isTrue(begin == null || end == null || !begin.isAfter(end),
                WorkflowCode.TASK_QUERY_INVALID, "时间区间非法");
    }

    /** 集合参数归一化：去空、去重。 */
    public static List<Long> normalizeIds(List<Long> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).distinct().toList();
    }

    /** 必填参数校验：空白即拒绝，通过后返回去空白值。 */
    public static String normalizeRequired(String value, String message) {
        Require.isTrue(StringUtils.hasText(value), WorkflowCode.TASK_QUERY_INVALID, message);
        return value.trim();
    }

    /** 读取当前租户 ID，缺失或非数字均拒绝。 */
    public static Long currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.isTrue(StringUtils.hasText(tenantId), WorkflowCode.TASK_QUERY_INVALID, "缺少租户上下文");
        String normalized = Objects.requireNonNull(tenantId).trim();
        Require.isTrue(normalized.chars().allMatch(Character::isDigit),
                WorkflowCode.TASK_QUERY_INVALID, "租户上下文非法");
        return Long.valueOf(normalized);
    }
}
