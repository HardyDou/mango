package io.mango.workflow.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务申请状态动作。
 */
@Getter
@AllArgsConstructor
public enum WorkflowApplyAction {

    CREATE("创建申请"),
    SUBMIT("提交申请"),
    START_PROCESS("启动流程"),
    TASK_CREATED("任务到达"),
    APPROVE("审批通过"),
    REJECT("审批驳回"),
    WITHDRAW("撤回"),
    CANCEL("取消"),
    TERMINATE("终止"),
    COMPLETE("流程完成");

    private final String label;
}
