package io.mango.workflow.core.service.impl;

import io.mango.common.result.Require;
import io.mango.workflow.api.enums.WorkflowCode;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.core.service.WorkflowTaskAdvanceResult;
import io.mango.workflow.core.support.WorkflowReturnTargetSupport;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/** 退回时指定目标办理人的解析与指派，隔离 {@link WorkflowTaskRuntimeService} 的可选分支。 */
final class WorkflowReturnTaskAssigner {

    private WorkflowReturnTaskAssigner() {
    }

    /** 解析并校验目标办理人；未指定时返回 null，指定后必须指向当前租户启用成员。 */
    static String resolveTargetAssignee(String input,
                                       Function<String, Long> userIdResolver,
                                       Function<Long, Long> memberIdResolver) {
        if (input == null) {
            return null;
        }
        Long userId = Require.nonNull(userIdResolver.apply(input), WorkflowCode.TASK_INVALID,
                "退回目标办理人不存在或已停用");
        Require.isTrue(memberIdResolver.apply(userId) != null, WorkflowCode.TASK_INVALID,
                "退回目标办理人不是当前租户的启用成员");
        return String.valueOf(userId);
    }

    /** 记录退回前目标节点已有任务 ID；未指定办理人时返回空集合。 */
    static Set<String> existingTargetTaskIds(TaskService taskService, String processInstanceId,
                                             String targetTaskDefinitionKey, String targetAssigneeUserId) {
        return targetAssigneeUserId == null ? Set.of()
                : WorkflowReturnTargetSupport.targetTaskIds(taskService, processInstanceId, targetTaskDefinitionKey);
    }

    /** 指定了办理人时，在本次退回新建的目标任务上写入 assignee 并刷新业务当前任务。 */
    static WorkflowTaskAdvanceResult assignIfRequested(WorkflowTaskAdvanceResult current,
                                                       TaskService taskService,
                                                       String processInstanceId,
                                                       String targetTaskDefinitionKey,
                                                       Set<String> existingTaskIds,
                                                       String targetAssigneeUserId,
                                                       Runnable syncAssignees,
                                                       Supplier<WorkflowBusinessApplyVO> refreshCurrentTasks) {
        if (targetAssigneeUserId == null) {
            return current;
        }
        Task targetTask = WorkflowReturnTargetSupport.requireNewTargetTask(
                taskService, processInstanceId, targetTaskDefinitionKey, existingTaskIds);
        taskService.setAssignee(targetTask.getId(), targetAssigneeUserId);
        targetTask.setAssignee(targetAssigneeUserId);
        syncAssignees.run();
        return new WorkflowTaskAdvanceResult(processInstanceId, false, refreshCurrentTasks.get());
    }
}
