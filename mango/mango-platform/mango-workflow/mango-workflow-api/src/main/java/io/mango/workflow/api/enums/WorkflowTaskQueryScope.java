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
     * 按组织范围过滤任务；已下线，传入会以 {@code TASK_QUERY_INVALID} 拒绝。
     *
     * <p>部门档改由平台数据范围配置下发（资源码 {@code workflow:task:list}），业务不需要也不应该
     * 在请求里指定部门：范围必须由登录人的角色配置决定，否则「前端传什么就能查什么」。
     */
    ORG;
}
