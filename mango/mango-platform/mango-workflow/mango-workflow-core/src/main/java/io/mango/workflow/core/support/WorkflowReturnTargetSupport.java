package io.mango.workflow.core.support;

import io.mango.common.result.Require;
import io.mango.workflow.api.enums.WorkflowCode;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 退回目标解析与退回生效校验。
 * <p>
 * 退回使用流程引擎的变更活动状态能力，不依赖源节点到目标节点的回退连线。目标只接受当前流程实例中已完成过的用户任务节点：
 * 目标本身是用户任务时直接使用；目标为网关、汇聚或服务任务时，按流程定义向上游解析最近的已完成用户任务，因为把执行移动到
 * 汇聚网关会被网关直接带回原节点。生效判断以运行时任务为准：目标节点必须有待办任务，且不能停留在原节点。
 */
public final class WorkflowReturnTargetSupport {

    /** 退回记录中保存目标节点的变量名。 */
    public static final String TARGET_VARIABLE = "targetTaskDefinitionKey";

    /** 退回记录中保存目标办理人的变量名。 */
    public static final String TARGET_ASSIGNEE_VARIABLE = "targetAssigneeUserId";

    private WorkflowReturnTargetSupport() {
    }

    /** 解析退回目标：优先使用显式配置，否则取最近一个已完成的不同用户任务节点。 */
    public static String resolveTarget(HistoryService historyService, RepositoryService repositoryService,
                                       Task task, String configuredTarget) {
        String target = configuredTarget == null ? "" : configuredTarget.trim();
        if (!StringUtils.hasText(target)) {
            return latestFinishedTarget(historyService, task);
        }
        if (isFinishedUserTask(historyService, task, target)) {
            requireNotCurrentNode(task, target);
            return target;
        }
        return resolveUpstreamTarget(historyService, repositoryService, task, target);
    }

    private static String latestFinishedTarget(HistoryService historyService, Task task) {
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .list();
        for (HistoricTaskInstance historicTask : historicTasks) {
            String taskDefinitionKey = taskDefinitionKey(historicTask);
            if (StringUtils.hasText(taskDefinitionKey) && !taskDefinitionKey.equals(task.getTaskDefinitionKey())) {
                return taskDefinitionKey;
            }
        }
        Require.isTrue(false, WorkflowCode.TASK_INVALID, "没有可退回的历史节点");
        return "";
    }

    private static String resolveUpstreamTarget(HistoryService historyService, RepositoryService repositoryService,
                                                Task task, String target) {
        BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        FlowElement targetElement = model == null || model.getMainProcess() == null
                ? null
                : model.getMainProcess().getFlowElement(target);
        Set<String> candidates = targetElement == null
                ? Set.of()
                : upstreamUserTasks(model, targetElement, finishedTaskKeys(historyService, task));
        Require.isTrue(candidates.size() <= 1, WorkflowCode.TASK_INVALID,
                "目标节点「" + target + "」不是用户任务，其上游存在多个可退回节点："
                        + String.join(", ", candidates) + "，请直接指定其中一个");
        Require.isTrue(!candidates.isEmpty(), WorkflowCode.TASK_INVALID,
                "目标节点「" + target + "」不是用户任务，且其上游没有可退回的已完成用户任务；"
                        + "可直接指定已完成的历史用户任务节点");
        String resolved = candidates.iterator().next();
        requireNotCurrentNode(task, resolved);
        return resolved;
    }

    private static boolean isFinishedUserTask(HistoryService historyService, Task task, String taskDefinitionKey) {
        long count = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .taskDefinitionKey(taskDefinitionKey)
                .finished()
                .count();
        return count > 0;
    }

    private static Set<String> finishedTaskKeys(HistoryService historyService, Task task) {
        Set<String> taskDefinitionKeys = new LinkedHashSet<>();
        for (HistoricTaskInstance historicTask : historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .finished()
                .list()) {
            String taskDefinitionKey = taskDefinitionKey(historicTask);
            if (StringUtils.hasText(taskDefinitionKey) && !taskDefinitionKey.equals(task.getTaskDefinitionKey())) {
                taskDefinitionKeys.add(taskDefinitionKey);
            }
        }
        return taskDefinitionKeys;
    }

    /** 沿入参连线向上游广度优先查找已完成用户任务，最多收集两个候选用于区分唯一与歧义。 */
    private static Set<String> upstreamUserTasks(BpmnModel model, FlowElement targetElement,
                                                 Set<String> finishedTaskKeys) {
        Set<String> candidates = new LinkedHashSet<>();
        Deque<FlowElement> pending = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        pending.add(targetElement);
        while (!pending.isEmpty() && candidates.size() < 2) {
            FlowElement current = pending.poll();
            if (current == null || !visited.add(current.getId())) {
                continue;
            }
            for (FlowElement source : upstreamElements(model, current)) {
                if (source instanceof UserTask userTask) {
                    collectCandidate(candidates, userTask, finishedTaskKeys);
                } else if (!(source instanceof StartEvent)) {
                    pending.add(source);
                }
            }
        }
        return candidates;
    }

    private static void collectCandidate(Set<String> candidates, UserTask userTask, Set<String> finishedTaskKeys) {
        if (finishedTaskKeys.contains(userTask.getId())) {
            candidates.add(userTask.getId());
        }
    }

    private static List<FlowElement> upstreamElements(BpmnModel model, FlowElement element) {
        List<FlowElement> sources = new ArrayList<>();
        for (FlowElement candidate : model.getMainProcess().getFlowElements()) {
            if (candidate instanceof SequenceFlow flow && element.getId().equals(flow.getTargetRef())) {
                FlowElement source = model.getMainProcess().getFlowElement(flow.getSourceRef());
                if (source != null) {
                    sources.add(source);
                }
            }
        }
        return sources;
    }

    private static void requireNotCurrentNode(Task task, String targetTaskDefinitionKey) {
        Require.isTrue(!targetTaskDefinitionKey.equals(task.getTaskDefinitionKey()),
                WorkflowCode.TASK_INVALID, "不能退回当前节点");
    }

    private static String taskDefinitionKey(HistoricTaskInstance historicTask) {
        return historicTask.getTaskDefinitionKey() == null ? "" : historicTask.getTaskDefinitionKey().trim();
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

    /** 退回前目标节点已有的任务 ID，用于识别本次退回新建的任务。 */
    public static Set<String> targetTaskIds(TaskService taskService, String processInstanceId,
                                            String targetTaskDefinitionKey) {
        Set<String> taskIds = new LinkedHashSet<>();
        for (Task targetTask : targetTasks(taskService, processInstanceId, targetTaskDefinitionKey)) {
            taskIds.add(targetTask.getId());
        }
        return taskIds;
    }

    /** 退回并指定办理人时，本次退回必须在目标节点只新建一条可办理任务。 */
    public static Task requireNewTargetTask(TaskService taskService, String processInstanceId,
                                            String targetTaskDefinitionKey, Set<String> existingTaskIds) {
        Set<String> existing = Require.nonNull(existingTaskIds, WorkflowCode.TASK_INVALID,
                "退回前目标节点任务快照不能为空");
        List<Task> createdTasks = targetTasks(taskService, processInstanceId, targetTaskDefinitionKey).stream()
                .filter(targetTask -> !existing.contains(targetTask.getId()))
                .toList();
        Require.isTrue(!createdTasks.isEmpty(), WorkflowCode.TASK_INVALID,
                "退回目标节点没有产生可办理任务，不能指定目标办理人");
        Require.isTrue(createdTasks.size() == 1, WorkflowCode.TASK_INVALID,
                "退回目标节点产生了多条办理任务，不能指定目标办理人");
        return createdTasks.getFirst();
    }

    private static List<Task> targetTasks(TaskService taskService, String processInstanceId,
                                          String targetTaskDefinitionKey) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(targetTaskDefinitionKey)
                .list();
    }

    /** 退回办理记录使用的变量快照。 */
    public static Map<String, Object> returnVariables(Map<String, Object> variables, String targetTaskDefinitionKey,
                                                      String targetAssigneeUserId) {
        Map<String, Object> result = new LinkedHashMap<>(variables == null ? Map.of() : variables);
        result.put(TARGET_VARIABLE, targetTaskDefinitionKey);
        if (StringUtils.hasText(targetAssigneeUserId)) {
            result.put(TARGET_ASSIGNEE_VARIABLE, targetAssigneeUserId.trim());
        }
        return result;
    }
}
