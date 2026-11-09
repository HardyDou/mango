package io.mango.workflow.core.support;

import io.mango.common.result.Require;
import io.mango.workflow.api.enums.WorkflowCode;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 退回目标解析与退回生效校验。
 * <p>
 * 退回使用流程引擎的变更活动状态能力，不依赖源节点到目标节点的回退连线。目标只接受当前流程实例中已完成过的用户任务节点，
 * 生效判断以运行时任务为准：目标节点必须有待办任务，且不能停留在原节点。
 */
public final class WorkflowReturnTargetSupport {

    /** 退回记录中保存目标节点的变量名。 */
    public static final String TARGET_VARIABLE = "targetTaskDefinitionKey";

    private WorkflowReturnTargetSupport() {
    }

    /** 解析退回目标：优先使用显式配置，否则取最近一个已完成的不同用户任务节点。 */
    public static String resolveTarget(HistoryService historyService, Task task, String configuredTarget) {
        String target = configuredTarget == null ? "" : configuredTarget.trim();
        if (StringUtils.hasText(target)) {
            ensureHistoricTarget(historyService, task, target);
            return target;
        }
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .list();
        for (HistoricTaskInstance historicTask : historicTasks) {
            String taskDefinitionKey = historicTask.getTaskDefinitionKey() == null
                    ? "" : historicTask.getTaskDefinitionKey().trim();
            if (StringUtils.hasText(taskDefinitionKey) && !taskDefinitionKey.equals(task.getTaskDefinitionKey())) {
                return taskDefinitionKey;
            }
        }
        Require.isTrue(false, WorkflowCode.TASK_INVALID, "没有可退回的历史节点");
        return "";
    }

    private static void ensureHistoricTarget(HistoryService historyService, Task task, String targetTaskDefinitionKey) {
        long count = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .taskDefinitionKey(targetTaskDefinitionKey)
                .finished()
                .count();
        Require.isTrue(count > 0, WorkflowCode.TASK_INVALID, "目标退回节点不在已完成历史中");
        Require.isTrue(!targetTaskDefinitionKey.equals(task.getTaskDefinitionKey()),
                WorkflowCode.TASK_INVALID, "不能退回当前节点");
    }

    /**
     * 引擎变更活动状态后立即确认目标节点产生了可办理任务。没有则整笔退回事务回滚，避免调用方以为退回成功。
     */
    public static void ensureTargetTaskCreated(TaskService taskService, Task sourceTask, String targetTaskDefinitionKey) {
        long targetTaskCount = taskService.createTaskQuery()
                .processInstanceId(sourceTask.getProcessInstanceId())
                .taskDefinitionKey(targetTaskDefinitionKey)
                .count();
        Require.isTrue(targetTaskCount > 0, WorkflowCode.TASK_INVALID,
                "退回未生效：目标节点「" + targetTaskDefinitionKey + "」没有产生可办理任务");
    }

    /**
     * 目标节点若因审批人为空自动通过，流程会再次经过原节点，调用方无法区分“退回成功”和“退回没动”。
     * 流程未结束、目标节点无任务且原节点又有任务时属于自相矛盾的退回结果，必须失败。
     */
    public static void ensureLeftSourceNode(TaskService taskService, Task sourceTask, String targetTaskDefinitionKey,
                                            boolean ended) {
        if (ended) {
            return;
        }
        List<Task> currentTasks = taskService.createTaskQuery()
                .processInstanceId(sourceTask.getProcessInstanceId())
                .list();
        boolean targetPending = currentTasks.stream()
                .anyMatch(currentTask -> targetTaskDefinitionKey.equals(currentTask.getTaskDefinitionKey()));
        boolean sourcePending = currentTasks.stream()
                .anyMatch(currentTask -> sourceTask.getTaskDefinitionKey().equals(currentTask.getTaskDefinitionKey()));
        Require.isTrue(targetPending || !sourcePending, WorkflowCode.TASK_INVALID,
                "退回未生效：目标节点「" + targetTaskDefinitionKey + "」没有待办任务，流程仍停在原节点「"
                        + sourceTask.getTaskDefinitionKey() + "」");
    }

    /** 退回办理记录使用的变量快照。 */
    public static Map<String, Object> returnVariables(Map<String, Object> variables, String targetTaskDefinitionKey) {
        Map<String, Object> result = new LinkedHashMap<>(variables == null ? Map.of() : variables);
        result.put(TARGET_VARIABLE, targetTaskDefinitionKey);
        return result;
    }
}
