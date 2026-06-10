package io.mango.job.api.enums;

/**
 * Job Worker 在线状态。
 */
public enum JobWorkerStatus {

    /**
     * 已注册。
     */
    REGISTERED,

    /**
     * 在线。
     */
    ONLINE,

    /**
     * 排空中。
     */
    DRAINING,

    /**
     * 离线。
     */
    OFFLINE,

    /**
     * 心跳过期。
     */
    EXPIRED,

    /**
     * 已禁用。
     */
    DISABLED,

    /**
     * 状态未知。
     */
    UNKNOWN
}
