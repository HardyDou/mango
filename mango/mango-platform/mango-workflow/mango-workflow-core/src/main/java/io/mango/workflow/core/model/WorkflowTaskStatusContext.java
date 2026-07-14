package io.mango.workflow.core.model;

/** 业务申请任务状态迁移所需的内部上下文。 */
public record WorkflowTaskStatusContext(
        String processInstanceId,
        String comment,
        String taskId,
        String taskDefinitionKey) {
}
