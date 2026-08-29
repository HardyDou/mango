package io.mango.workflow.core.service.impl;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.workflow.api.enums.WorkflowAutoAssignmentStrategy;
import org.flowable.task.api.Task;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Selects an assignee for an AUTO workflow task. */
final class WorkflowAutoAssignmentSelector {

    private final JdbcTemplate jdbcTemplate;

    WorkflowAutoAssignmentSelector(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Long select(Task task, List<Long> candidates, Long previous, WorkflowAutoAssignmentStrategy strategy) {
        WorkflowAutoAssignmentStrategy resolved = strategy == null
                ? WorkflowAutoAssignmentStrategy.ROUND_ROBIN : strategy;
        if (resolved == WorkflowAutoAssignmentStrategy.AFFINITY) {
            Long affinity = affinityCandidate(task, candidates);
            if (affinity != null) {
                return affinity;
            }
        }
        if (resolved == WorkflowAutoAssignmentStrategy.LEAST_TASKS
                || resolved == WorkflowAutoAssignmentStrategy.AFFINITY) {
            Map<Long, Long> taskCounts = new java.util.LinkedHashMap<>();
            for (Long candidate : candidates) {
                taskCounts.put(candidate, activeTaskCount(candidate));
            }
            return selectLeastTasksCandidate(candidates, taskCounts);
        }
        return WorkflowTaskRuntimeService.nextRoundRobinCandidate(candidates, previous);
    }

    static Long selectLeastTasksCandidate(List<Long> candidates, Map<Long, Long> taskCounts) {
        return candidates.stream()
                .min(Comparator.comparingLong((Long candidate) -> taskCounts.getOrDefault(candidate, 0L))
                        .thenComparingLong(Long::longValue))
                .orElse(null);
    }

    long activeTaskCount(Long userId) {
        Long count = jdbcTemplate.queryForObject("""
                select count(*)
                from ACT_RU_TASK task
                join workflow_form_instance form on form.process_instance_id = task.PROC_INST_ID_
                where task.ASSIGNEE_ = ? and form.tenant_id = ?
                """, Long.class, String.valueOf(userId), MangoContextHolder.tenantId());
        return count == null ? 0L : count;
    }

    Long affinityCandidate(Task task, List<Long> candidates) {
        List<Long> recent = jdbcTemplate.query("""
                select user_id
                from workflow_process_participant
                where tenant_id = ? and process_instance_id = ?
                  and participant_type = 'COMPLETED_HANDLER'
                  and active = 1 and user_id is not null
                order by last_participated_at desc, id desc
                """, (resultSet, rowNum) -> resultSet.getLong("user_id"),
                MangoContextHolder.tenantId(), task.getProcessInstanceId());
        return selectAffinityCandidate(candidates, recent);
    }

    static Long selectAffinityCandidate(List<Long> candidates, List<Long> recentHandlers) {
        Set<Long> candidateSet = new HashSet<>(candidates);
        return recentHandlers.stream().filter(candidateSet::contains).findFirst().orElse(null);
    }
}
