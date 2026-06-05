package io.mango.job.api.enums;

/**
 * Job 调度类型。
 */
public enum JobScheduleType {

    /**
     * Cron 表达式调度。
     */
    CRON,

    /**
     * 固定频率调度。
     */
    FIXED_RATE,

    /**
     * 一次性调度。
     */
    ONE_TIME,

    /**
     * 仅手动触发。
     */
    MANUAL
}
