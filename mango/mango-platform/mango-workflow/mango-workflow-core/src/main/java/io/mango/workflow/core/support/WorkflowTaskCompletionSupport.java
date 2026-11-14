package io.mango.workflow.core.support;

import io.mango.common.result.Require;
import io.mango.workflow.api.enums.WorkflowCode;
import io.mango.workflow.api.enums.WorkflowTaskAction;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 任务完成入口的并发冲突处理。
 */
public final class WorkflowTaskCompletionSupport {

    private WorkflowTaskCompletionSupport() {
    }

    /**
     * 返回仍可办理的任务；运行态任务已消失时区分“已被办理”和“不存在”。
     */
    public static Task requireCompletable(TaskService taskService, JdbcTemplate jdbcTemplate,
                                          Long tenantId, String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task != null) {
            return task;
        }
        if (hasCompletedRecord(jdbcTemplate, tenantId, taskId)) {
            return Require.fail(WorkflowCode.TASK_ALREADY_COMPLETED);
        }
        return Require.fail(WorkflowCode.TASK_NOT_FOUND);
    }

    /**
     * 认领并完成引擎任务；并发失败且完成记录已落库时转换为明确的已办理业务错误。
     */
    public static void complete(TaskService taskService, JdbcTemplate jdbcTemplate, Long tenantId,
                                Task task, Map<String, Object> variables, String comment, Runnable claimAction) {
        try {
            if (StringUtils.hasText(comment)) {
                taskService.addComment(task.getId(), task.getProcessInstanceId(), comment.trim());
            }
            claimAction.run();
            taskService.complete(task.getId(), variables);
        } catch (FlowableException | DataIntegrityViolationException e) {
            if (hasCompletedRecordForUpdate(jdbcTemplate, tenantId, task.getId())) {
                Require.fail(WorkflowCode.TASK_ALREADY_COMPLETED, WorkflowCode.TASK_ALREADY_COMPLETED.getMessage(), e);
            }
            Require.rethrow(e);
        }
    }

    private static boolean hasCompletedRecord(JdbcTemplate jdbcTemplate, Long tenantId, String taskId) {
        Long count = jdbcTemplate.queryForObject("""
                select count(*)
                from workflow_task_record
                where tenant_id = ? and task_id = ? and action = ?
                """, Long.class, tenantId, taskId, WorkflowTaskAction.COMPLETE.name());
        return count != null && count > 0;
    }

    /**
     * 使用当前读确认完成记录，避免 REPEATABLE READ 快照看不到并发事务刚提交的记录。
     */
    private static boolean hasCompletedRecordForUpdate(JdbcTemplate jdbcTemplate, Long tenantId, String taskId) {
        return !jdbcTemplate.queryForList("""
                select id
                from workflow_task_record
                where tenant_id = ? and task_id = ? and action = ?
                for update
                """, Long.class, tenantId, taskId, WorkflowTaskAction.COMPLETE.name()).isEmpty();
    }
}
