package io.mango.workflow.core.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 按业务主键查询任务的持久化条件。
 *
 * <p>由服务层把 API 查询对象转换成该内部条件后再交给 Mapper，避免 API 协议模型进入持久化层。
 */
@Getter
@Builder
public class WorkflowTaskHistoryCriteria {

    private final Long tenantId;
    private final String businessType;
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
    private final boolean selfScope;
    private final List<String> selfIdentifiers;
    private final List<String> selfCandidateGroups;
    private final List<Long> deniedApplyIds;
    private final String sortColumn;
    private final String sortDirection;
}
