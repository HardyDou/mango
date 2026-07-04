package io.mango.notice.api.enums;

/**
 * 系统消息动作请求流水状态。
 */
public enum NoticeSiteMessageActionRequestStatus {

    /**
     * 已请求，等待业务模块消费。
     */
    REQUESTED,

    /**
     * 业务模块已处理成功。
     */
    SUCCEEDED,

    /**
     * 业务模块处理失败。
     */
    FAILED
}
