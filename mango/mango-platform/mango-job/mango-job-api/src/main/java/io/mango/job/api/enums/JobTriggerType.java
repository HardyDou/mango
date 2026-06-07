package io.mango.job.api.enums;

/**
 * Job 触发来源。
 */
public enum JobTriggerType {

    /**
     * 调度触发。
     */
    SCHEDULED,

    /**
     * 手动触发。
     */
    MANUAL,

    /**
     * 重试触发。
     */
    RETRY,

    /**
     * API 触发。
     */
    API
}
