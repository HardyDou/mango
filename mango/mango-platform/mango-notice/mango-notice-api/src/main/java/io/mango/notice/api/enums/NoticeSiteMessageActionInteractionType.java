package io.mango.notice.api.enums;

/**
 * 系统消息动作交互类型。
 */
public enum NoticeSiteMessageActionInteractionType {

    /**
     * 当前消息动作提交后投递领域事件。
     */
    EVENT,

    /**
     * 当前消息动作只进入业务处理页面，不直接改变业务状态。
     */
    ROUTE
}
