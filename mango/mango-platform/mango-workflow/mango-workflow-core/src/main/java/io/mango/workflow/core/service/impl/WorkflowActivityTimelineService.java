package io.mango.workflow.core.service.impl;

import io.mango.common.result.Require;
import io.mango.workflow.api.enums.WorkflowActivityKind;
import io.mango.workflow.api.enums.WorkflowCode;
import io.mango.workflow.api.query.WorkflowActivityTimelineQuery;
import io.mango.workflow.api.vo.WorkflowActivityTimelineVO;
import io.mango.workflow.core.config.WorkflowTaskQueryProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 按业务键读取活动级时间轴。
 *
 * <p>数据来源是 Flowable 历史活动表（经 {@link HistoryService} 读取），不经过
 * {@code workflow_task_record}：任务流水只记人工处理动作，网关与事件不会落库，
 * 因此活动时间轴与流转记录是两条互补的时间线。
 *
 * <p>租户解析、业务键收窄、时间区间与参数归一化直接复用
 * {@link WorkflowTaskBusinessQueryService} 的包内方法，保证与流转记录同一口径，
 * 不另立一份实现。
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
    private final WorkflowTaskBusinessQueryService businessQueryService;
    private final WorkflowTaskQueryProperties properties;

    /**
     * 读取业务键下全部流程实例的活动时间轴。
     *
     * <p>返回该业务键命中的每个流程实例的全部活动（不只网关），按开始时间、活动 ID 升序；
     * 业务类型与业务键必填，部门集合仅用于收窄命中的流程实例。
     *
     * <p>时间区间只过滤<b>活动开始时间</b>：进行中的活动（结束时间为空）按开始时间参与过滤，
     * 不会按完成时间筛选，这与流转记录接口按记录创建时间过滤的口径不同。
     *
     * @param request 查询条件
     * @return 活动行列表；业务键未登记流程实例时返回空集合
     */
    public List<WorkflowActivityTimelineVO> activityTimelineByBusinessKey(WorkflowActivityTimelineQuery request) {
        WorkflowActivityTimelineQuery resolved = Require.nonNull(request, WorkflowCode.TASK_QUERY_INVALID,
                "活动时间轴查询条件不能为空");
        String businessType = businessQueryService.normalizeRequired(resolved.getBusinessType(), "业务类型不能为空");
        String businessKey = businessQueryService.normalizeRequired(resolved.getBusinessKey(), "业务主键不能为空");
        businessQueryService.validateTimeRange(resolved.getBeginTime(), resolved.getEndTime());
        Long tenantId = businessQueryService.currentTenantId();
        List<Long> deptIds = businessQueryService.normalizeIds(resolved.getDeptIds());
        List<String> processInstanceIds = businessQueryService.processInstanceIds(tenantId, businessType,
                businessKey, deptIds);
        if (processInstanceIds.isEmpty()) {
            return List.of();
        }
        int limit = Math.max(properties.getUnpagedMaxRows(), 1);
        List<WorkflowActivityTimelineVO> activities = new ArrayList<>();
        for (String processInstanceId : processInstanceIds) {
            for (HistoricActivityInstance instance : historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .list()) {
                LocalDateTime startTime = toLocalDateTime(instance.getStartTime());
                if (!inRange(startTime, resolved.getBeginTime(), resolved.getEndTime())) {
                    continue;
                }
                activities.add(toVo(processInstanceId, instance, startTime));
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

    private WorkflowActivityTimelineVO toVo(String processInstanceId, HistoricActivityInstance instance,
                                           LocalDateTime startTime) {
        WorkflowActivityTimelineVO vo = new WorkflowActivityTimelineVO();
        vo.setProcessInstanceId(processInstanceId);
        vo.setActivityId(instance.getActivityId());
        vo.setActivityName(instance.getActivityName());
        vo.setActivityType(instance.getActivityType());
        vo.setKind(kindOf(instance.getActivityType()));
        vo.setAssignee(instance.getAssignee());
        vo.setStartTime(startTime);
        vo.setEndTime(toLocalDateTime(instance.getEndTime()));
        vo.setDurationMillis(instance.getDurationInMillis());
        return vo;
    }

    /**
     * 按 Flowable 活动类型原值归类；判断大小写不敏感，未知类型归入 {@link WorkflowActivityKind#OTHER}。
     */
    private WorkflowActivityKind kindOf(String activityType) {
        if (StringUtils.isBlank(activityType)) {
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

    /**
     * Flowable 历史活动的起止时间来自数据库 {@code DATETIME}，按 JVM 时区读出的 {@link java.util.Date}
     * 与写入同源，这里用同一时区转本地时间，往返一致。
     */
    private LocalDateTime toLocalDateTime(java.util.Date value) {
        return value == null ? null : LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
    }

    /** 时间区间只作用于活动开始时间；开始时间为空的活动不参与返回。 */
    private boolean inRange(LocalDateTime startTime, LocalDateTime beginTime, LocalDateTime endTime) {
        if (startTime == null) {
            return false;
        }
        return (beginTime == null || !startTime.isBefore(beginTime))
                && (endTime == null || !startTime.isAfter(endTime));
    }
}
