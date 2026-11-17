package io.mango.workflow.core.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 按业务类型查询任务的持久化条件，业务主键可选。
 *
 * <p>由服务层把 API 查询对象转换成该内部条件后再交给 Mapper，避免 API 协议模型进入持久化层。
 */
@Getter
@Builder
public class WorkflowTaskHistoryCriteria {

    private final Long tenantId;
    private final String businessType;
    /** 多业务类型过滤；为空时沿用 {@link #businessType} 等值过滤，保证 1.12.0 的 SQL 不变。 */
    private final List<String> businessTypes;
    /** 流程定义 Key 过滤（业务申请表 process_definition_key），为空表示不限。 */
    private final List<String> processDefinitionKeys;
    private final List<String> businessKeys;
    private final List<String> taskDefinitionKeys;
    private final List<Long> assigneeIds;
    private final List<String> assigneeIdentifiers;
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
}
