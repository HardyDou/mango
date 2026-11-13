package io.mango.workflow.core.service.impl;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.workflow.api.enums.WorkflowAutoAssignmentStrategy;
import io.mango.workflow.api.enums.WorkflowNodeAffinityFallback;
import org.flowable.task.api.Task;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Selects an assignee for an AUTO workflow task. */
final class WorkflowAutoAssignmentSelector {

    private final JdbcTemplate jdbcTemplate;

    WorkflowAutoAssignmentSelector(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Long select(Task task, List<Long> candidates, Long previous, WorkflowAutoAssignmentStrategy strategy) {
        return select(task, candidates, previous, strategy, WorkflowNodeAffinityFallback.CLAIM);
    }

    Long select(Task task, List<Long> candidates, Long previous, WorkflowAutoAssignmentStrategy strategy,
                WorkflowNodeAffinityFallback nodeAffinityFallback) {
        WorkflowAutoAssignmentStrategy resolved = strategy == null
                ? WorkflowAutoAssignmentStrategy.ROUND_ROBIN : strategy;
        if (resolved == WorkflowAutoAssignmentStrategy.NODE_AFFINITY) {
            Long nodeAffinity = nodeAffinityCandidate(task, candidates);
            return nodeAffinity != null
                    ? nodeAffinity
                    : fallbackCandidate(candidates, previous, nodeAffinityFallback);
        }
        if (resolved == WorkflowAutoAssignmentStrategy.AFFINITY) {
            Long affinity = affinityCandidate(task, candidates);
            if (affinity != null) {
                return affinity;
            }
        }
        if (resolved == WorkflowAutoAssignmentStrategy.LEAST_TASKS
                || resolved == WorkflowAutoAssignmentStrategy.AFFINITY) {
            return leastTasksCandidate(candidates);
        }
        return WorkflowTaskRuntimeService.nextRoundRobinCandidate(candidates, previous);
    }

    private Long fallbackCandidate(List<Long> candidates, Long previous, WorkflowNodeAffinityFallback fallback) {
        WorkflowNodeAffinityFallback resolved = fallback == null
                ? WorkflowNodeAffinityFallback.CLAIM : fallback;
        return switch (resolved) {
            case CLAIM -> null;
            case RANDOM -> randomCandidate(candidates);
            case ROUND_ROBIN -> WorkflowTaskRuntimeService.nextRoundRobinCandidate(candidates, previous);
            case LEAST_TASKS -> leastTasksCandidate(candidates);
        };
    }

    Long leastTasksCandidate(List<Long> candidates) {
        Map<Long, Long> taskCounts = new java.util.LinkedHashMap<>();
        for (Long candidate : candidates) {
            taskCounts.put(candidate, activeTaskCount(candidate));
        }
        return selectLeastTasksCandidate(candidates, taskCounts);
    }

    static Long randomCandidate(List<Long> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
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

    Long nodeAffinityCandidate(Task task, List<Long> candidates) {
        List<Long> handlers = jdbcTemplate.query("""
                select operator_id
                from workflow_task_record
                where tenant_id = ? and process_instance_id = ? and task_definition_key = ?
                  and action in ('CLAIM', 'COMPLETE', 'REJECT', 'RETURN')
                  and operator_id is not null
                order by created_time asc, id asc
                """, (resultSet, rowNum) -> resultSet.getLong("operator_id"),
                MangoContextHolder.tenantId(), task.getProcessInstanceId(), task.getTaskDefinitionKey());
        return selectNodeAffinityCandidate(candidates, handlers);
    }

    static Long selectNodeAffinityCandidate(List<Long> candidates, List<Long> historyHandlers) {
        return selectAffinityCandidate(candidates, historyHandlers);
    }

    static Long selectAffinityCandidate(List<Long> candidates, List<Long> recentHandlers) {
        Set<Long> candidateSet = new HashSet<>(candidates);
        return recentHandlers.stream().filter(candidateSet::contains).findFirst().orElse(null);
    }
}
