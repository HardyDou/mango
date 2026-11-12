package io.mango.workflow.api.enums;

/**
 * 任务查询数据范围。
 */
public enum WorkflowTaskQueryScope {

    /**
     * 仅本人任务：办理人是当前用户，或当前用户是候选人或候选组匹配。
     */
    SELF,

    /**
     * 当前租户内该业务主键下的全部任务；需要 workflow:task:all 权限码。
     */
    ALL,

    /**
     * 按指定组织范围过滤任务所属申请单；当前版本未开放，传入将拒绝。
     */
    ORG;
}
