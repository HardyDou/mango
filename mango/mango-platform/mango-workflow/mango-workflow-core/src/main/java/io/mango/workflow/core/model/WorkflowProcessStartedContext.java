package io.mango.workflow.core.model;

/** 业务申请关联流程实例所需的内部上下文。 */
public record WorkflowProcessStartedContext(
        Long applyId,
        Long processDefinitionId,
        String processDefinitionKey,
        String engineProcessDefinitionId,
        String processName,
        String processInstanceId) {
}
