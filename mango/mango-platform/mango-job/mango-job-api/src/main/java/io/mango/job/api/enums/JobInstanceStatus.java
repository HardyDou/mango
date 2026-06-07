package io.mango.job.api.enums;

/**
 * Job 实例状态。
 */
public enum JobInstanceStatus {

    /**
     * 已创建。
     */
    CREATED,

    /**
     * 等待执行。
     */
    WAITING,

    /**
     * 已分发。
     */
    DISPATCHED,

    /**
     * 执行中。
     */
    RUNNING,

    /**
     * 等待重试。
     */
    RETRY_WAITING,

    /**
     * 执行成功。
     */
    SUCCESS,

    /**
     * 执行失败。
     */
    FAILED,

    /**
     * 执行超时。
     */
    TIMEOUT,

    /**
     * 已取消。
     */
    CANCELED
}
