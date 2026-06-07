package io.mango.job.api.enums;

/**
 * Job 执行尝试状态。
 */
public enum JobAttemptStatus {

    /**
     * 待租约分发。
     */
    READY,

    /**
     * 已授予执行租约。
     */
    LEASED,

    /**
     * 执行中。
     */
    RUNNING,

    /**
     * 执行成功。
     */
    SUCCEEDED,

    /**
     * 执行失败。
     */
    FAILED,

    /**
     * 执行超时。
     */
    TIMED_OUT,

    /**
     * Worker 失联或租约丢失。
     */
    LOST,

    /**
     * 已取消。
     */
    CANCELED
}
