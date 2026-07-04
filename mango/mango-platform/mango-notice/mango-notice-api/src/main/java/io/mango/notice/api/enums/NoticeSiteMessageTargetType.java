package io.mango.notice.api.enums;

/**
 * 系统消息跳转目标类型。
 */
public enum NoticeSiteMessageTargetType {

    /**
     * 无跳转目标。
     */
    NONE,

    /**
     * 前端应用内命名路由。
     */
    ROUTE,

    /**
     * 业务自定义交互流程。
     */
    FLOW
}
