package io.mango.workflow.api;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工作流模块业务码。
 */
@Getter
@AllArgsConstructor
public enum WorkflowCode implements BizCode {

    /** 流程分组不存在。 */
    GROUP_NOT_FOUND(3601, "流程分组不存在"),

    /** 流程分组参数非法。 */
    GROUP_INVALID(3602, "流程分组参数非法"),

    /** 流程定义不存在。 */
    DEFINITION_NOT_FOUND(3610, "流程定义不存在"),

    /** 流程定义参数非法。 */
    DEFINITION_INVALID(3611, "流程定义参数非法"),

    /** 流程定义编码重复。 */
    DEFINITION_KEY_DUPLICATED(3612, "流程定义编码已存在"),

    /** 流程定义状态非法。 */
    DEFINITION_STATUS_INVALID(3613, "流程定义状态非法"),

    /** 流程设计器内容非法。 */
    DESIGNER_INVALID(3614, "流程设计器内容非法"),

    /** 流程发布版本不存在。 */
    VERSION_NOT_FOUND(3615, "流程发布版本不存在"),

    /** 流程实例不存在。 */
    PROCESS_INSTANCE_NOT_FOUND(3640, "流程实例不存在"),

    /** 业务申请不存在。 */
    APPLY_NOT_FOUND(3641, "业务申请不存在"),

    /** 业务申请参数非法。 */
    APPLY_INVALID(3642, "业务申请参数非法"),

    /** 流程任务不存在。 */
    TASK_NOT_FOUND(3650, "流程任务不存在"),

    /** 流程任务参数非法。 */
    TASK_INVALID(3651, "流程任务参数非法"),

    /** 流程发布失败。 */
    DEPLOY_FAILED(3620, "流程发布失败");

    private final int code;
    private final String message;
}
