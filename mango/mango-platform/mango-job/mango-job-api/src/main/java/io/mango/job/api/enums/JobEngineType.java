package io.mango.job.api.enums;

/**
 * Job 底层调度引擎类型。
 */
public enum JobEngineType {

    /**
     * PowerJob 引擎。
     */
    POWERJOB,

    /**
     * XXL-JOB 引擎。
     */
    XXL_JOB,

    /**
     * Quartz 内嵌引擎。
     */
    QUARTZ
}
