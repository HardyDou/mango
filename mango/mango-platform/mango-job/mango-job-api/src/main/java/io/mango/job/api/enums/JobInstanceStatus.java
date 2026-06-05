package io.mango.job.api.enums;

/**
 * Job 实例状态。
 */
public enum JobInstanceStatus {

    /**
     * 等待执行。
     */
    WAITING,

    /**
     * 执行中。
     */
    RUNNING,

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
