package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.mango.workflow.api.enums.WorkflowCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.workflow.api.enums.WorkflowTaskCancelCause;
import io.mango.workflow.api.enums.WorkflowTaskClaimStatus;
import io.mango.workflow.api.enums.WorkflowTaskQueryScope;
import io.mango.workflow.api.enums.WorkflowTaskRuntimeStatus;
import io.mango.workflow.api.query.WorkflowTaskBusinessPageQuery;
import io.mango.workflow.api.query.WorkflowTaskTraceQuery;
import io.mango.workflow.api.enums.WorkflowApplyAction;
import io.mango.workflow.api.enums.WorkflowTaskAction;
import io.mango.workflow.api.vo.WorkflowTaskRecordVO;
import io.mango.workflow.api.vo.WorkflowTaskVO;
import io.mango.workflow.core.convert.WorkflowTaskRecordConverter;
import io.mango.workflow.core.entity.WorkflowTaskRecordEntity;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyStatusLogMapper;
import io.mango.workflow.core.mapper.WorkflowTaskRecordMapper;
import io.mango.workflow.core.entity.WorkflowBusinessApplyStatusLogEntity;
import io.mango.workflow.core.config.WorkflowTaskQueryProperties;
import io.mango.workflow.core.engine.WorkflowCandidateGroupProvider;
import io.mango.workflow.core.entity.WorkflowBusinessApplyEntity;
import io.mango.workflow.core.identity.WorkflowAssigneeIdentityService;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyMapper;
import io.mango.workflow.core.mapper.WorkflowTaskHistoryMapper;
import io.mango.workflow.core.model.WorkflowTaskHistoryCriteria;
import io.mango.workflow.core.model.WorkflowTaskHistoryRow;
import io.mango.workflow.core.model.WorkflowTaskIdentityLinkRow;
import io.mango.workflow.core.service.IWorkflowTaskBusinessQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 按业务类型查询任务实现，业务主键可选。
 *
 * <p>查询直接读取 Flowable 历史任务表并关联业务申请单，因此可以跨处理人返回同一业务类型下的全部任务，
 * 同时把租户、业务主键和数据范围收敛在 SQL 层完成，不做内存过滤。
 * <p>流转记录读取（{@code traceByBusinessKey}）只做精确查询，不参与数据范围与申请级权限判定。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowTaskBusinessQueryService implements IWorkflowTaskBusinessQueryService {

    private static final int PAGE_SIZE_UPPER_BOUND = 500;
    private static final String RETURN_TARGET_VARIABLE = "targetTaskDefinitionKey";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final WorkflowTaskHistoryMapper taskHistoryMapper;
    private final WorkflowBusinessApplyMapper businessApplyMapper;
    private final WorkflowTaskRecordMapper taskRecordMapper;
    private final WorkflowBusinessApplyStatusLogMapper statusLogMapper;
    private final ObjectProvider<WorkflowBusinessApplyAccessChecker> accessCheckerProvider;
    private final WorkflowTaskQueryAuthorizer authorizer;
    private final WorkflowAssigneeIdentityService assigneeIdentityService;
    private final WorkflowCandidateGroupProvider candidateGroupProvider;
    private final WorkflowTaskQueryProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<WorkflowTaskVO> pageByBusinessKey(WorkflowTaskBusinessPageQuery request) {
        WorkflowTaskBusinessPageQuery resolved =
                Require.nonNull(request, WorkflowCode.TASK_QUERY_INVALID, "任务查询条件不能为空");
        WorkflowTaskQueryScope scope = resolved.getScope() == null
                ? WorkflowTaskQueryScope.SELF
                : resolved.getScope();
        validateScope(resolved, scope);
        validateTimeRange(resolved.getCreateTimeBegin(), resolved.getCreateTimeEnd());
        validateTimeRange(resolved.getEndTimeBegin(), resolved.getEndTimeEnd());
        validatePaging(resolved);
        String businessType = normalizeRequired(resolved.getBusinessType(), "业务类型不能为空");
        List<String> businessKeys = normalizeBusinessKeys(resolved.getBusinessKeys());
        Long tenantId = currentTenantId();
        boolean selfScope = scope == WorkflowTaskQueryScope.SELF;
        List<String> selfIdentifiers = selfScope ? currentUserIdentifiers() : List.of();
        List<String> selfGroups = selfScope ? candidateGroups() : List.of();
        if (selfScope) {
            Require.isTrue(!selfIdentifiers.isEmpty(), WorkflowCode.TASK_QUERY_INVALID, "缺少当前用户上下文");
        }
        WorkflowTaskHistoryCriteria criteria = WorkflowTaskHistoryCriteria.builder()
                .tenantId(tenantId)
                .businessType(businessType)
                .businessKeys(businessKeys)
                .taskDefinitionKeys(normalizeStrings(resolved.getTaskDefinitionKeys()))
                .assigneeIds(normalizeIds(resolved.getAssigneeIds()))
                .assigneeIdentifiers(assigneeIdentifiers(resolved.getAssigneeIds()))
                .includeTodo(includeStatus(resolved, WorkflowTaskRuntimeStatus.TODO))
                .includeDone(includeStatus(resolved, WorkflowTaskRuntimeStatus.DONE))
                .includeCanceled(includeStatus(resolved, WorkflowTaskRuntimeStatus.CANCELED))
                .assigneeRequired(assigneeRequired(resolved))
                .applyStatuses(applyStatuses(resolved))
                .returned(resolved.getReturned())
                .createTimeBegin(resolved.getCreateTimeBegin())
                .createTimeEnd(resolved.getCreateTimeEnd())
                .endTimeBegin(resolved.getEndTimeBegin())
                .endTimeEnd(resolved.getEndTimeEnd())
                .keyword(keywordPattern(resolved.getKeyword()))
                .applyKeyword(applyKeywordPattern(resolved.getApplyKeyword()))
                .selfScope(selfScope)
                .selfIdentifiers(selfIdentifiers)
                .selfCandidateGroups(selfGroups)
                .deniedApplyIds(deniedApplyIds(tenantId, businessType, businessKeys))
                .sortColumn(resolved.getSortBy())
                .sortDirection(resolved.getSortOrder())
                .build();
        long total = taskHistoryMapper.countTasks(criteria);
        int limit = resolveLimit(resolved);
        long offset = resolveOffset(resolved);
        List<WorkflowTaskVO> records = total == 0L
                ? List.of()
                : toRecords(taskHistoryMapper.selectTasks(criteria, limit, offset));
        if (!records.isEmpty()) {
            assigneeIdentityService.enrichTasks(records);
        }
        return PageResult.of(records, total, resolvePage(resolved), resolveSize(resolved, records));
    }

    @Override
    public List<WorkflowTaskRecordVO> traceByBusinessKey(WorkflowTaskTraceQuery request) {
        WorkflowTaskTraceQuery resolved = Require.nonNull(request, WorkflowCode.TASK_QUERY_INVALID,
                "流转记录查询条件不能为空");
        String businessType = normalizeRequired(resolved.getBusinessType(), "业务类型不能为空");
        String businessKey = normalizeRequired(resolved.getBusinessKey(), "业务主键不能为空");
        validateTimeRange(resolved.getBeginTime(), resolved.getEndTime());
        List<String> actions = normalizeActions(resolved.getActions());
        List<Long> operatorIds = normalizeIds(resolved.getOperatorIds());
        Long tenantId = currentTenantId();
        List<String> processInstanceIds = processInstanceIds(tenantId, businessType, businessKey,
                normalizeIds(resolved.getDeptIds()));
        if (processInstanceIds.isEmpty()) {
            return List.of();
        }
        List<String> operatorNames = operatorIds.isEmpty() ? List.of() : operatorNames(operatorIds, tenantId);
        int limit = Math.max(properties.getUnpagedMaxRows(), 1);
        List<WorkflowTaskRecordVO> records = new ArrayList<>(taskRecordMapper.selectList(traceWrapper(
                        processInstanceIds, operatorIds, operatorNames, actions, resolved.getBeginTime(),
                        resolved.getEndTime(), limit))
                .stream()
                .map(record -> WorkflowTaskRecordConverter.toVo(record, null))
                .toList());
        records.addAll(withdrawRecords(processInstanceIds, operatorIds, operatorNames, actions,
                resolved.getBeginTime(), resolved.getEndTime()));
        Require.isTrue(records.size() <= limit, WorkflowCode.TASK_QUERY_INVALID,
                "流转记录超过单次返回上限（" + limit + "），请缩小时间区间后重试");
        records.sort(Comparator.comparing(WorkflowTaskRecordVO::getCreatedTime)
                .thenComparing(WorkflowTaskRecordVO::getId));
        return List.copyOf(records);
    }

    /** 撤回只写申请状态流水；读取时并入同一时间轴，覆盖升级前已撤回的历史申请。 */
    private List<WorkflowTaskRecordVO> withdrawRecords(List<String> processInstanceIds, List<Long> operatorIds,
                                                       List<String> operatorNames, List<String> actions,
                                                       LocalDateTime beginTime, LocalDateTime endTime) {
        if (!actions.isEmpty() && !actions.contains(WorkflowTaskAction.WITHDRAW.name())) {
            return List.of();
        }
        LambdaQueryWrapper<WorkflowBusinessApplyStatusLogEntity> wrapper =
                new LambdaQueryWrapper<WorkflowBusinessApplyStatusLogEntity>()
                        .select(WorkflowBusinessApplyStatusLogEntity::getId,
                                WorkflowBusinessApplyStatusLogEntity::getProcessInstanceId,
                                WorkflowBusinessApplyStatusLogEntity::getTaskId,
                                WorkflowBusinessApplyStatusLogEntity::getTaskDefinitionKey,
                                WorkflowBusinessApplyStatusLogEntity::getAction,
                                WorkflowBusinessApplyStatusLogEntity::getActionName,
                                WorkflowBusinessApplyStatusLogEntity::getOperatorId,
                                WorkflowBusinessApplyStatusLogEntity::getOperatorName,
                                WorkflowBusinessApplyStatusLogEntity::getComment,
                                WorkflowBusinessApplyStatusLogEntity::getCreatedAt)
                        .in(WorkflowBusinessApplyStatusLogEntity::getProcessInstanceId, processInstanceIds)
                        .eq(WorkflowBusinessApplyStatusLogEntity::getAction, WorkflowApplyAction.WITHDRAW.name())
                        .ge(beginTime != null, WorkflowBusinessApplyStatusLogEntity::getCreatedAt, beginTime)
                        .le(endTime != null, WorkflowBusinessApplyStatusLogEntity::getCreatedAt, endTime);
        if (!operatorIds.isEmpty()) {
            wrapper.and(nested -> nested
                    .in(WorkflowBusinessApplyStatusLogEntity::getOperatorId, operatorIds)
                    .or()
                    .in(WorkflowBusinessApplyStatusLogEntity::getOperatorName, operatorNames));
        }
        return statusLogMapper.selectList(wrapper).stream()
                .map(WorkflowTaskRecordConverter::toVo)
                .toList();
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
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private LambdaQueryWrapper<WorkflowTaskRecordEntity> traceWrapper(List<String> processInstanceIds,
                                                                     List<Long> operatorIds,
                                                                     List<String> operatorNames,
                                                                     List<String> actions, LocalDateTime beginTime,
                                                                     LocalDateTime endTime, int limit) {
        LambdaQueryWrapper<WorkflowTaskRecordEntity> wrapper = new LambdaQueryWrapper<WorkflowTaskRecordEntity>()
                .select(WorkflowTaskRecordEntity::getId,
                        WorkflowTaskRecordEntity::getProcessInstanceId,
                        WorkflowTaskRecordEntity::getTaskId,
                        WorkflowTaskRecordEntity::getTaskName,
                        WorkflowTaskRecordEntity::getTaskDefinitionKey,
                        WorkflowTaskRecordEntity::getAction,
                        WorkflowTaskRecordEntity::getActionName,
                        WorkflowTaskRecordEntity::getOperatorId,
                        WorkflowTaskRecordEntity::getOperatorName,
                        WorkflowTaskRecordEntity::getComment,
                        WorkflowTaskRecordEntity::getCreatedTime)
                .in(WorkflowTaskRecordEntity::getProcessInstanceId, processInstanceIds)
                .ge(beginTime != null, WorkflowTaskRecordEntity::getCreatedTime, beginTime)
                .le(endTime != null, WorkflowTaskRecordEntity::getCreatedTime, endTime)
                .in(!actions.isEmpty(), WorkflowTaskRecordEntity::getAction, actions)
                .orderByAsc(WorkflowTaskRecordEntity::getCreatedTime)
                .orderByAsc(WorkflowTaskRecordEntity::getId)
                .last("limit " + (limit + 1));
        if (!operatorIds.isEmpty()) {
            wrapper.and(nested -> nested.in(WorkflowTaskRecordEntity::getOperatorId, operatorIds)
                    .or()
                    .in(WorkflowTaskRecordEntity::getOperatorName, operatorNames));
        }
        return wrapper;
    }

    /** 处理人名称可能是登录名，也可能是字符串化用户 ID；两者都参与匹配。 */
    private List<String> operatorNames(List<Long> operatorIds, Long tenantId) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        operatorIds.forEach(userId -> names.add(String.valueOf(userId)));
        names.addAll(taskHistoryMapper.selectUserNamesByIds(
                operatorIds.stream().map(String::valueOf).toList(), String.valueOf(tenantId)));
        return List.copyOf(names);
    }

    private List<String> normalizeActions(List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            return List.of();
        }
        return actions.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(value -> {
                    WorkflowTaskAction action = WorkflowTaskAction.fromCode(value);
                    Require.notNull(action, WorkflowCode.TASK_QUERY_INVALID, "未知审批动作：" + value);
                    return action.name();
                })
                .distinct()
                .toList();
    }

    private void validateScope(WorkflowTaskBusinessPageQuery query, WorkflowTaskQueryScope scope) {
        Require.isTrue(scope != WorkflowTaskQueryScope.ORG,
                WorkflowCode.TASK_QUERY_INVALID, "指定组织范围暂不支持");
        Require.isTrue(query.getDeptIds() == null || query.getDeptIds().isEmpty(),
                WorkflowCode.TASK_QUERY_INVALID, "deptIds 仅指定组织范围可用");
        if (scope == WorkflowTaskQueryScope.ALL) {
            Require.isTrue(authorizer.canQueryAll(), WorkflowCode.TASK_QUERY_FORBIDDEN);
        }
    }

    private void validateTimeRange(LocalDateTime begin, LocalDateTime end) {
        Require.isTrue(begin == null || end == null || !begin.isAfter(end),
                WorkflowCode.TASK_QUERY_INVALID, "时间区间非法");
    }

    private void validatePaging(WorkflowTaskBusinessPageQuery query) {
        boolean pagePresent = query.getPage() != null;
        boolean sizePresent = query.getSize() != null;
        Require.isTrue(pagePresent == sizePresent,
                WorkflowCode.TASK_QUERY_INVALID, "分页参数必须成对出现");
        Require.isTrue(!sizePresent || query.getSize() <= PAGE_SIZE_UPPER_BOUND,
                WorkflowCode.TASK_QUERY_INVALID, "每页数量不能超过500");
    }

    private List<Long> deniedApplyIds(Long tenantId, String businessType, List<String> businessKeys) {
        WorkflowBusinessApplyAccessChecker checker = accessCheckerProvider.getIfAvailable();
        if (businessKeys.isEmpty() || checker == null || !checker.hasProvider(businessType)) {
            return List.of();
        }
        List<WorkflowBusinessApplyEntity> applies = businessApplyMapper.selectList(
                new LambdaQueryWrapper<WorkflowBusinessApplyEntity>()
                        .eq(WorkflowBusinessApplyEntity::getTenantId, String.valueOf(tenantId))
                        .eq(WorkflowBusinessApplyEntity::getBusinessType, businessType)
                        .in(WorkflowBusinessApplyEntity::getBusinessKey, businessKeys));
        return applies.stream()
                .filter(apply -> !checker.isAllowed(apply))
                .map(WorkflowBusinessApplyEntity::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    private int resolveLimit(WorkflowTaskBusinessPageQuery query) {
        if (query.getPage() != null && query.getSize() != null) {
            return query.getSize();
        }
        return Math.max(properties.getUnpagedMaxRows(), 1);
    }

    private long resolveOffset(WorkflowTaskBusinessPageQuery query) {
        if (query.getPage() == null || query.getSize() == null) {
            return 0L;
        }
        return (long) (query.getPage() - 1) * query.getSize();
    }

    private int resolvePage(WorkflowTaskBusinessPageQuery query) {
        return query.getPage() == null ? 1 : query.getPage();
    }

    private int resolveSize(WorkflowTaskBusinessPageQuery query, List<WorkflowTaskVO> records) {
        return query.getSize() == null ? records.size() : query.getSize();
    }

    private boolean includeStatus(WorkflowTaskBusinessPageQuery query, WorkflowTaskRuntimeStatus status) {
        if (query.getStatuses() == null || query.getStatuses().isEmpty()) {
            return true;
        }
        return query.getStatuses().contains(status);
    }

    private Boolean assigneeRequired(WorkflowTaskBusinessPageQuery query) {
        if (query.getClaimStatuses() == null || query.getClaimStatuses().isEmpty()) {
            return null;
        }
        boolean assigned = query.getClaimStatuses().contains(WorkflowTaskClaimStatus.ASSIGNED);
        boolean unclaimed = query.getClaimStatuses().contains(WorkflowTaskClaimStatus.UNCLAIMED)
                || query.getClaimStatuses().contains(WorkflowTaskClaimStatus.NONE);
        if (assigned && unclaimed) {
            return null;
        }
        return assigned;
    }

    private List<String> applyStatuses(WorkflowTaskBusinessPageQuery query) {
        if (query.getApplyStatuses() == null || query.getApplyStatuses().isEmpty()) {
            return List.of();
        }
        return query.getApplyStatuses().stream().map(Enum::name).toList();
    }

    private String keywordPattern(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return "%" + keyword.trim() + "%";
    }

    /**
     * 申请关键字转为 LIKE 模式，% 和 _ 按字面匹配，避免调用方输入被当成通配符扩大命中范围。
     */
    private String applyKeywordPattern(String applyKeyword) {
        if (!StringUtils.hasText(applyKeyword)) {
            return null;
        }
        String escaped = applyKeyword.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private List<WorkflowTaskVO> toRecords(List<WorkflowTaskHistoryRow> rows) {
        List<String> taskIds = rows.stream().map(WorkflowTaskHistoryRow::getTaskId).toList();
        Map<String, List<WorkflowTaskIdentityLinkRow>> linksByTask = taskIds.isEmpty()
                ? Map.of()
                : taskHistoryMapper.selectCandidateLinks(taskIds).stream()
                        .collect(Collectors.groupingBy(WorkflowTaskIdentityLinkRow::getTaskId,
                                LinkedHashMap::new, Collectors.toList()));
        List<WorkflowTaskVO> records = new ArrayList<>(rows.size());
        for (WorkflowTaskHistoryRow row : rows) {
            records.add(toVo(row, linksByTask.getOrDefault(row.getTaskId(), List.of())));
        }
        return records;
    }

    private WorkflowTaskVO toVo(WorkflowTaskHistoryRow row, List<WorkflowTaskIdentityLinkRow> links) {
        WorkflowTaskVO vo = new WorkflowTaskVO();
        vo.setId(row.getTaskId());
        vo.setTaskName(row.getTaskName());
        vo.setTaskDefinitionKey(row.getTaskDefinitionKey());
        vo.setProcessInstanceId(row.getProcessInstanceId());
        vo.setProcessDefinitionId(row.getProcessDefinitionId());
        vo.setProcessName(row.getProcessName());
        vo.setProcessKey(row.getProcessKey());
        vo.setBusinessKey(row.getBusinessKey());
        vo.setApplyId(row.getApplyId());
        vo.setApplyStatus(row.getApplyStatus());
        vo.setOrgId(row.getOrgId());
        vo.setReturned(Boolean.TRUE.equals(row.getReturned()));
        vo.setAssigneeName(row.getAssigneeKey());
        vo.setCreateTime(row.getCreateTime());
        vo.setClaimTime(row.getClaimTime());
        vo.setEndTime(row.getEndTime());
        vo.setDeleteReason(row.getDeleteReason());
        vo.setStatus(resolveStatus(row).name());
        vo.setCancelCause(resolveCancelCause(row));
        vo.setLastReturnAt(row.getLastReturnAt());
        vo.setLastReturnTarget(resolveLastReturnTarget(row));
        vo.setClaimStatus(StringUtils.hasText(row.getAssigneeKey())
                ? WorkflowTaskClaimStatus.ASSIGNED
                : WorkflowTaskClaimStatus.UNCLAIMED);
        vo.setCandidateUsers(links.stream()
                .map(WorkflowTaskIdentityLinkRow::getUserId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList());
        vo.setCandidateGroups(links.stream()
                .map(WorkflowTaskIdentityLinkRow::getGroupId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList());
        vo.setClaimable(Boolean.FALSE);
        vo.setUnclaimable(Boolean.FALSE);
        return vo;
    }

    private String resolveCancelCause(WorkflowTaskHistoryRow row) {
        if (resolveStatus(row) != WorkflowTaskRuntimeStatus.CANCELED) {
            return null;
        }
        if (Boolean.TRUE.equals(row.getTaskReturned())) {
            return WorkflowTaskCancelCause.RETURNED.name();
        }
        if (WorkflowApplyStatus.WITHDRAWN.name().equals(row.getApplyStatus())) {
            return WorkflowTaskCancelCause.WITHDRAWN.name();
        }
        if (WorkflowApplyStatus.TERMINATED.name().equals(row.getApplyStatus())
                || WorkflowApplyStatus.CANCELED.name().equals(row.getApplyStatus())) {
            return WorkflowTaskCancelCause.TERMINATED.name();
        }
        return WorkflowTaskCancelCause.OTHER.name();
    }

    private String resolveLastReturnTarget(WorkflowTaskHistoryRow row) {
        if (!StringUtils.hasText(row.getLastReturnVariables())) {
            return null;
        }
        try {
            Map<String, Object> variables = objectMapper.readValue(row.getLastReturnVariables(), MAP_TYPE);
            Object target = variables.get(RETURN_TARGET_VARIABLE);
            return target == null ? null : String.valueOf(target);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse workflow return variables for apply {}", row.getApplyId(), ex);
            return null;
        }
    }

    private WorkflowTaskRuntimeStatus resolveStatus(WorkflowTaskHistoryRow row) {
        if (StringUtils.hasText(row.getDeleteReason())) {
            return WorkflowTaskRuntimeStatus.CANCELED;
        }
        return row.getEndTime() == null ? WorkflowTaskRuntimeStatus.TODO : WorkflowTaskRuntimeStatus.DONE;
    }

    private List<String> normalizeBusinessKeys(List<String> businessKeys) {
        if (businessKeys == null) {
            return List.of();
        }
        return businessKeys.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private List<String> normalizeStrings(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private List<Long> normalizeIds(List<Long> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).distinct().toList();
    }

    private List<String> assigneeIdentifiers(List<Long> assigneeIds) {
        if (assigneeIds == null) {
            return List.of();
        }
        return assigneeIds.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .distinct()
                .toList();
    }

    private String normalizeRequired(String value, String message) {
        Require.isTrue(StringUtils.hasText(value), WorkflowCode.TASK_QUERY_INVALID, message);
        return value.trim();
    }

    private Long currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.isTrue(StringUtils.hasText(tenantId), WorkflowCode.TASK_QUERY_INVALID, "缺少租户上下文");
        String normalized = Objects.requireNonNull(tenantId).trim();
        Require.isTrue(normalized.chars().allMatch(Character::isDigit),
                WorkflowCode.TASK_QUERY_INVALID, "租户上下文非法");
        return Long.valueOf(normalized);
    }

    private List<String> currentUserIdentifiers() {
        LinkedHashSet<String> identifiers = new LinkedHashSet<>();
        if (StringUtils.hasText(MangoContextHolder.principalName())) {
            identifiers.add(MangoContextHolder.principalName());
        }
        Long userId = MangoContextHolder.userId();
        if (userId != null) {
            identifiers.add(String.valueOf(userId));
        }
        return List.copyOf(identifiers);
    }

    private List<String> candidateGroups() {
        List<String> groups = candidateGroupProvider.currentCandidateGroups();
        return groups == null ? List.of() : groups.stream().filter(StringUtils::hasText).toList();
    }
}
