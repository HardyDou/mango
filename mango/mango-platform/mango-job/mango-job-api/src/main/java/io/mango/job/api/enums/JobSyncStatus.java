package io.mango.job.api.enums;

/**
 * Mango Job 与底层引擎同步状态。
 */
public enum JobSyncStatus {

    /**
     * 待同步。
     */
    PENDING,

    /**
     * 已同步。
     */
    SYNCED,

    /**
     * 同步失败。
     */
    FAILED
}
