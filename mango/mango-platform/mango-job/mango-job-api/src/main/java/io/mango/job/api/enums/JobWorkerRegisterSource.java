package io.mango.job.api.enums;

/**
 * Job Worker 注册来源。
 */
public enum JobWorkerRegisterSource {

    /**
     * 单体或同进程部署下，由 Mango Job 运行时根据本 JVM 的真实处理器自动注册。
     */
    EMBEDDED_AUTO,

    /**
     * 远程 Worker 通过心跳向 JobCenter 自动注册。
     */
    REMOTE_AUTO,

    /**
     * 管理后台手动登记的远程 Worker。
     */
    MANUAL
}
