package io.mango.job.api.enums;

/**
 * Job 处理器类型。
 */
public enum JobType {

    /**
     * 当前应用内置 Spring Bean 处理器。
     */
    BUILTIN,

    /**
     * 远程 Mango 服务处理器。
     */
    REMOTE_API,

    /**
     * 受控 HTTP 任务。
     */
    HTTP,

    /**
     * 脚本任务，首轮不启用。
     */
    SCRIPT,

    /**
     * 引擎原生任务。
     */
    ENGINE_NATIVE
}
