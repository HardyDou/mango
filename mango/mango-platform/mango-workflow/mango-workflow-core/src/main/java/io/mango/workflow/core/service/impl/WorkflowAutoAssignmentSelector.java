package io.mango.workflow.core.service.impl;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.workflow.api.enums.WorkflowAutoAssignmentStrategy;
import io.mango.workflow.api.enums.WorkflowNodeAffinityFallback;
import io.mango.workflow.api.enums.WorkflowNodeAffinityScope;
import org.flowable.task.api.Task;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

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

    /**
     * 自动派单结果。
     *
     * @param userId 选中用户；{@code null} 表示保持待领取
     * @param affinityScope 命中亲和的层级；{@code null} 表示本次没有命中亲和
     */
    record Decision(Long userId, WorkflowNodeAffinityScope affinityScope) {
    }

    Long select(Task task, List<Long> candidates, Long previous, WorkflowAutoAssignmentStrategy strategy) {
        return select(task, candidates, previous, strategy, WorkflowNodeAffinityFallback.CLAIM);
    }

    Long select(Task task, List<Long> candidates, Long previous, WorkflowAutoAssignmentStrategy strategy,
                WorkflowNodeAffinityFallback nodeAffinityFallback) {
        return decide(task, candidates, previous, strategy, nodeAffinityFallback,
                WorkflowNodeAffinityScope.PROCESS_INSTANCE).userId();
    }

    Decision decide(Task task, List<Long> candidates, Long previous, WorkflowAutoAssignmentStrategy strategy,
                    WorkflowNodeAffinityFallback nodeAffinityFallback,
                    WorkflowNodeAffinityScope nodeAffinityScope) {
        WorkflowAutoAssignmentStrategy resolved = strategy == null
                ? WorkflowAutoAssignmentStrategy.ROUND_ROBIN : strategy;
        if (resolved == WorkflowAutoAssignmentStrategy.NODE_AFFINITY) {
            Decision nodeAffinity = nodeAffinityDecision(task, candidates, nodeAffinityScope);
            if (nodeAffinity != null) {
                return nodeAffinity;
            }
            return new Decision(fallbackCandidate(candidates, previous, nodeAffinityFallback), null);
        }
        if (resolved == WorkflowAutoAssignmentStrategy.AFFINITY) {
            Long affinity = affinityCandidate(task, candidates);
            if (affinity != null) {
                return new Decision(affinity, null);
            }
        }
        if (resolved == WorkflowAutoAssignmentStrategy.LEAST_TASKS
                || resolved == WorkflowAutoAssignmentStrategy.AFFINITY) {
            return new Decision(leastTasksCandidate(candidates), null);
        }
        return new Decision(WorkflowTaskRuntimeService.nextRoundRobinCandidate(candidates, previous), null);
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
        return selectNodeAffinityCandidate(candidates, instanceNodeHandlers(task));
    }

    /**
     * 按由近及远逐层选择节点亲和办理人：当前实例、同一业务键、同一亲和键。
     * 某一层查不到历史或历史办理人都不在候选集时继续下一层。
     */
    private Decision nodeAffinityDecision(Task task, List<Long> candidates, WorkflowNodeAffinityScope scope) {
        Long instanceHandler = selectNodeAffinityCandidate(candidates, instanceNodeHandlers(task));
        if (instanceHandler != null) {
            return new Decision(instanceHandler, WorkflowNodeAffinityScope.PROCESS_INSTANCE);
        }
        if (scope == null || scope == WorkflowNodeAffinityScope.PROCESS_INSTANCE) {
            return null;
        }
        WorkflowAffinityKeys keys = affinityKeys(task.getProcessInstanceId());
        if (keys == null) {
            return null;
        }
        Long businessKeyHandler = selectNodeAffinityCandidate(candidates, businessKeyNodeHandlers(task, keys));
        if (businessKeyHandler != null) {
            return new Decision(businessKeyHandler, WorkflowNodeAffinityScope.BUSINESS_KEY);
        }
        if (scope != WorkflowNodeAffinityScope.AFFINITY_KEY || !StringUtils.hasText(keys.affinityKey())) {
            return null;
        }
        Long affinityKeyHandler = selectNodeAffinityCandidate(candidates, affinityKeyNodeHandlers(task, keys));
        return affinityKeyHandler == null
                ? null : new Decision(affinityKeyHandler, WorkflowNodeAffinityScope.AFFINITY_KEY);
    }

    private List<Long> instanceNodeHandlers(Task task) {
        return jdbcTemplate.query("""
                select operator_id
                from workflow_task_record
                where tenant_id = ? and process_instance_id = ? and task_definition_key = ?
                  and action in ('CLAIM', 'COMPLETE', 'REJECT', 'RETURN')
                  and operator_id is not null
                order by created_time asc, id asc
                """, (resultSet, rowNum) -> resultSet.getLong("operator_id"),
                MangoContextHolder.tenantId(), task.getProcessInstanceId(), task.getTaskDefinitionKey());
    }

    /**
     * 同一业务键下的历史办理人；排除当前实例，当前实例由第一层负责。
     * 排序先最近实例、再实例内最早办理人，最多回溯 200 条记录。
     */
    private List<Long> businessKeyNodeHandlers(Task task, WorkflowAffinityKeys keys) {
        return jdbcTemplate.query("""
                select record.operator_id
                from workflow_task_record record
                join workflow_business_apply apply
                  on apply.tenant_id = record.tenant_id
                 and apply.process_instance_id = record.process_instance_id
                where record.tenant_id = ? and apply.business_type = ? and apply.business_key = ?
                  and record.process_instance_id <> ? and record.task_definition_key = ?
                  and record.action in ('CLAIM', 'COMPLETE', 'REJECT', 'RETURN')
                  and record.operator_id is not null
                order by apply.created_time desc, apply.id desc, record.created_time asc, record.id asc
                limit 200
                """, (resultSet, rowNum) -> resultSet.getLong("operator_id"),
                MangoContextHolder.tenantId(), keys.businessType(), keys.businessKey(),
                task.getProcessInstanceId(), task.getTaskDefinitionKey());
    }

    /**
     * 同一亲和键下的历史办理人；排除当前实例，当前实例由第一层负责。
     * 排序先最近实例、再实例内最早办理人，最多回溯 200 条记录。
     */
    private List<Long> affinityKeyNodeHandlers(Task task, WorkflowAffinityKeys keys) {
        return jdbcTemplate.query("""
                select record.operator_id
                from workflow_task_record record
                join workflow_business_apply apply
                  on apply.tenant_id = record.tenant_id
                 and apply.process_instance_id = record.process_instance_id
                where record.tenant_id = ? and apply.business_type = ? and apply.affinity_key = ?
                  and record.process_instance_id <> ? and record.task_definition_key = ?
                  and record.action in ('CLAIM', 'COMPLETE', 'REJECT', 'RETURN')
                  and record.operator_id is not null
                order by apply.created_time desc, apply.id desc, record.created_time asc, record.id asc
                limit 200
                """, (resultSet, rowNum) -> resultSet.getLong("operator_id"),
                MangoContextHolder.tenantId(), keys.businessType(), keys.affinityKey(),
                task.getProcessInstanceId(), task.getTaskDefinitionKey());
    }

    /** 读取当前实例登记的业务坐标；未登记业务的流程返回 {@code null}，调用方退回当前实例作用域。 */
    private WorkflowAffinityKeys affinityKeys(String processInstanceId) {
        if (!StringUtils.hasText(processInstanceId)) {
            return null;
        }
        List<WorkflowAffinityKeys> keys = jdbcTemplate.query("""
                select business_type, business_key, affinity_key
                from workflow_business_apply
                where tenant_id = ? and process_instance_id = ?
                order by id desc
                limit 1
                """, (resultSet, rowNum) -> new WorkflowAffinityKeys(
                resultSet.getString("business_type"),
                resultSet.getString("business_key"),
                resultSet.getString("affinity_key")),
                MangoContextHolder.tenantId(), processInstanceId);
        if (keys.isEmpty() || !StringUtils.hasText(keys.get(0).businessType())
                || !StringUtils.hasText(keys.get(0).businessKey())) {
            return null;
        }
        return keys.get(0);
    }

    static Long selectNodeAffinityCandidate(List<Long> candidates, List<Long> historyHandlers) {
        return selectAffinityCandidate(candidates, historyHandlers);
    }

    static Long selectAffinityCandidate(List<Long> candidates, List<Long> recentHandlers) {
        Set<Long> candidateSet = new HashSet<>(candidates);
        return recentHandlers.stream().filter(candidateSet::contains).findFirst().orElse(null);
    }

    /** 当前实例登记的业务坐标；亲和键可为空。 */
    private record WorkflowAffinityKeys(String businessType, String businessKey, String affinityKey) {
    }
}
