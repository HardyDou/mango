package io.mango.workflow.core.model;

import lombok.Builder;
import lombok.Getter;

import io.mango.workflow.api.enums.WorkflowTaskQueryStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 按业务类型查询任务的持久化条件，业务主键可选。
 *
 * <p>由服务层把 API 查询对象转换成该内部条件后再交给 Mapper，避免 API 协议模型进入持久化层。
 *
 * <p>业务类型有两层表达：{@code businessTypes} 非空时按集合匹配（{@code businessType} 被忽略），
 * 否则按 {@code businessType} 单值等值匹配，保证历史调用方的 SQL 不变。
 */
@Getter
@Builder
public class WorkflowTaskHistoryCriteria {

    private final Long tenantId;
    private final String businessType;
    private final List<String> businessTypes;
    private final List<String> businessKeys;
    private final List<String> taskDefinitionKeys;
    private final List<String> processDefinitionKeys;
    private final List<Long> assigneeIds;
    private final List<String> assigneeIdentifiers;
    private final List<Long> deptIds;
    private final List<String> queryStatuses;
    private final WorkflowTaskAssigneeScope assigneeScope;
    private final boolean includeTodo;
    private final boolean includeDone;
    private final boolean includeCanceled;
    private final Boolean assigneeRequired;
    private final List<String> applyStatuses;
    private final Boolean returned;
    private final LocalDateTime createTimeBegin;
    private final LocalDateTime createTimeEnd;
    private final LocalDateTime endTimeBegin;
    private final LocalDateTime endTimeEnd;
    private final String keyword;
    private final String applyKeyword;
    private final List<WorkflowApplyAttributeCondition> applyAttributeConditions;
    private final LocalDateTime applyTimeBegin;
    private final LocalDateTime applyTimeEnd;
    private final boolean selfScope;
    private final List<String> selfIdentifiers;
    private final List<String> selfCandidateGroups;
    private final List<Long> deniedApplyIds;
    private final String sortColumn;
    private final String sortDirection;

    /**
     * 状态谓词使用的细状态名。
     *
     * <p>三个布尔量是历史调用方的便捷入口（内部测试与旧调用点仍在使用）：
     * 未显式传 {@code queryStatuses} 时按分组展开成细状态，保证 SQL 只有一套谓词。
     * 分组语义：{@code includeDone} 含通过/驳回/退回，{@code includeCanceled} 含撤回/流程终止。
     *
     * @return 细状态名集合，空集合表示不限制状态
     */
    public List<String> getResolvedQueryStatuses() {
        if (queryStatuses != null && !queryStatuses.isEmpty()) {
            List<String> expanded = new ArrayList<>(queryStatuses.size());
            for (String queryStatus : queryStatuses) {
                WorkflowTaskQueryStatus status = WorkflowTaskQueryStatus.valueOf(queryStatus);
                for (WorkflowTaskQueryStatus member : status.expand()) {
                    if (!expanded.contains(member.name())) {
                        expanded.add(member.name());
                    }
                }
            }
            return coversEveryStatus(expanded) ? List.of() : List.copyOf(expanded);
        }
        List<String> statuses = new ArrayList<>(6);
        if (includeTodo) {
            statuses.add("TODO");
        }
        if (includeDone) {
            statuses.add("COMPLETED");
            statuses.add("REJECTED");
            statuses.add("RETURNED");
        }
        if (includeCanceled) {
            statuses.add("WITHDRAWN");
            statuses.add("TERMINATED");
        }
        return coversEveryStatus(statuses) ? List.of() : List.copyOf(statuses);
    }

    /**
     * 判断给定细状态是否已经覆盖全部状态。
     *
     * @param statuses 细状态名集合
     * @return 覆盖全部细状态时返回 true，此时不需要渲染状态谓词
     */
    private static boolean coversEveryStatus(List<String> statuses) {
        int detailCount = 0;
        for (WorkflowTaskQueryStatus status : WorkflowTaskQueryStatus.values()) {
            if (status.isGroup()) {
                continue;
            }
            detailCount++;
            if (!statuses.contains(status.name())) {
                return false;
            }
        }
        return statuses.size() == detailCount;
    }
}
