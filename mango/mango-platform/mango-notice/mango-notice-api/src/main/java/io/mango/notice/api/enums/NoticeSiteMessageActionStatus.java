package io.mango.notice.api.enums;

/**
 * 系统消息动作当前状态。
 */
public enum NoticeSiteMessageActionStatus {

    /**
     * 可执行。
     */
    AVAILABLE,

    /**
     * 已提交，等待业务处理。
     */
    PROCESSING,

    /**
     * 业务处理成功。
     */
    SUCCEEDED,

    /**
     * 业务处理失败。
     */
    FAILED,

    /**
     * 不可用。
     */
    DISABLED,

    /**
     * 已过期。
     */
    EXPIRED
}
