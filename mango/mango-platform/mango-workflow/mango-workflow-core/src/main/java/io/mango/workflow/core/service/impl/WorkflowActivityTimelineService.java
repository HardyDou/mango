package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.workflow.api.enums.WorkflowActivityKind;
import io.mango.workflow.api.enums.WorkflowCode;
import io.mango.workflow.api.query.WorkflowActivityTimelineQuery;
import io.mango.workflow.api.vo.WorkflowActivityTimelineVO;
import io.mango.workflow.core.config.WorkflowTaskQueryProperties;
import io.mango.workflow.core.entity.WorkflowBusinessApplyEntity;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 按业务键读取活动级时间轴。
 *
 * <p>数据来源是 Flowable 历史活动表（经 {@link HistoryService} 读取），不经过
 * {@code workflow_task_record}：任务流水只记人工处理动作，网关与事件不会落库，
 * 因此活动时间轴与流转记录是两条互补的时间线。
 */
@Service
@RequiredArgsConstructor
public class WorkflowActivityTimelineService {

    private static final Set<String> GATEWAY_TYPES = Set.of("exclusivegateway", "inclusivegateway",
            "parallelgateway", "eventbasedgateway", "complexgateway");

    private static final Set<String> TASK_TYPES = Set.of("usertask", "manualtask", "receivetask");

    private static final Set<String> EVENT_TYPES = Set.of("startevent", "endevent", "boundaryevent",
            "intermediatecatchevent", "intermediatethrowevent");

    private final HistoryService historyService;
    private final WorkflowBusinessApplyMapper businessApplyMapper;
    private final WorkflowTaskQueryProperties properties;

    /**
     * 读取业务键下全部流程实例的活动时间轴。
     *
     * <p>返回该业务键命中的每个流程实例的全部活动（不只网关），按开始时间、活动 ID 升序；
     * 业务类型与业务键必填，部门集合仅用于收窄命中的流程实例。
     *
     * @param request 查询条件
     * @return 活动行列表；业务键未登记流程实例时返回空集合
     */
    public List<WorkflowActivityTimelineVO> activityTimelineByBusinessKey(WorkflowActivityTimelineQuery request) {
        WorkflowActivityTimelineQuery resolved = Require.nonNull(request, WorkflowCode.TASK_QUERY_INVALID,
                "活动时间轴查询条件不能为空");
        String businessType = normalizeRequired(resolved.getBusinessType(), "业务类型不能为空");
        String businessKey = normalizeRequired(resolved.getBusinessKey(), "业务主键不能为空");
        validateTimeRange(resolved.getBeginTime(), resolved.getEndTime());
        Long tenantId = currentTenantId();
        List<Long> deptIds = normalizeIds(resolved.getDeptIds());
        List<String> processInstanceIds = processInstanceIds(tenantId, businessType, businessKey, deptIds);
        if (processInstanceIds.isEmpty()) {
            return List.of();
        }
        int limit = Math.max(properties.getUnpagedMaxRows(), 1);
        List<WorkflowActivityTimelineVO> activities = new ArrayList<>();
        for (String processInstanceId : processInstanceIds) {
            HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId);
            for (HistoricActivityInstance instance : query.list()) {
                if (!inRange(toLocalDateTime(instance.getStartTime()), resolved.getBeginTime(),
                        resolved.getEndTime())) {
                    continue;
                }
                activities.add(toVo(processInstanceId, instance));
            }
        }
        Require.isTrue(activities.size() <= limit, WorkflowCode.TASK_QUERY_INVALID,
                "活动时间轴超过单次返回上限（" + limit + "），请缩小时间区间后重试");
        activities.sort(Comparator.comparing(WorkflowActivityTimelineVO::getStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(WorkflowActivityTimelineVO::getActivityId,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return List.copyOf(activities);
    }

    /** Flowable 历史活动的起止时间是 {@link java.util.Date}，对外统一暴露本地时间。 */
    private LocalDateTime toLocalDateTime(java.util.Date value) {
        return value == null ? null : LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
    }

    private WorkflowActivityTimelineVO toVo(String processInstanceId, HistoricActivityInstance instance) {
        WorkflowActivityTimelineVO vo = new WorkflowActivityTimelineVO();
        vo.setProcessInstanceId(processInstanceId);
        vo.setActivityId(instance.getActivityId());
        vo.setActivityName(instance.getActivityName());
        vo.setActivityType(instance.getActivityType());
        vo.setKind(kindOf(instance.getActivityType()));
        vo.setAssignee(instance.getAssignee());
        vo.setStartTime(toLocalDateTime(instance.getStartTime()));
        vo.setEndTime(toLocalDateTime(instance.getEndTime()));
        vo.setDurationMillis(instance.getDurationInMillis());
        return vo;
    }

    /** 按 Flowable 活动类型原值归类；未知类型归入 {@link WorkflowActivityKind#OTHER}。 */
    private WorkflowActivityKind kindOf(String activityType) {
        if (!StringUtils.isNotBlank(activityType)) {
            return WorkflowActivityKind.OTHER;
        }
        String normalized = activityType.toLowerCase(Locale.ROOT);
        if (GATEWAY_TYPES.contains(normalized)) {
            return WorkflowActivityKind.GATEWAY;
        }
        if (TASK_TYPES.contains(normalized)) {
            return WorkflowActivityKind.TASK;
        }
        if (EVENT_TYPES.contains(normalized)) {
            return WorkflowActivityKind.EVENT;
        }
        return WorkflowActivityKind.OTHER;
    }

    private boolean inRange(LocalDateTime startTime, LocalDateTime beginTime, LocalDateTime endTime) {
        if (startTime == null) {
            return false;
        }
        return (beginTime == null || !startTime.isBefore(beginTime))
                && (endTime == null || !startTime.isAfter(endTime));
    }

    private void validateTimeRange(LocalDateTime beginTime, LocalDateTime endTime) {
        Require.isTrue(beginTime == null || endTime == null || !endTime.isBefore(beginTime),
                WorkflowCode.TASK_QUERY_INVALID, "结束时间不能早于开始时间");
    }

    private List<String> processInstanceIds(Long tenantId, String businessType, String businessKey,
                                            List<Long> deptIds) {
        return businessApplyMapper.selectList(new LambdaQueryWrapper<WorkflowBusinessApplyEntity>()
                        .select(WorkflowBusinessApplyEntity::getProcessInstanceId)
                        .eq(WorkflowBusinessApplyEntity::getTenantId, String.valueOf(tenantId))
                        .eq(WorkflowBusinessApplyEntity::getBusinessType, businessType)
                        .eq(WorkflowBusinessApplyEntity::getBusinessKey, businessKey)
                        .in(!deptIds.isEmpty(), WorkflowBusinessApplyEntity::getApplicantDeptId, deptIds)
                        .isNotNull(WorkflowBusinessApplyEntity::getProcessInstanceId))
                .stream()
                .map(WorkflowBusinessApplyEntity::getProcessInstanceId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }

    private Long currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.isTrue(StringUtils.isNotBlank(tenantId), WorkflowCode.TASK_QUERY_INVALID, "缺少租户上下文");
        String normalized = Objects.requireNonNull(tenantId).trim();
        Require.isTrue(normalized.chars().allMatch(Character::isDigit),
                WorkflowCode.TASK_QUERY_INVALID, "租户上下文非法");
        return Long.parseLong(normalized);
    }

    private List<Long> normalizeIds(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).distinct().toList();
    }

    private String normalizeRequired(String value, String message) {
        Require.isTrue(StringUtils.isNotBlank(value), WorkflowCode.TASK_QUERY_INVALID, message);
        return Objects.requireNonNull(value).trim();
    }
}
