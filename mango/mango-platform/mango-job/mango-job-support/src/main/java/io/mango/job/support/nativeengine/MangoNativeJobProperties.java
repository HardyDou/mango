package io.mango.job.support.nativeengine;

import io.mango.job.api.enums.JobTransportType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mango 原生 Job 运行时配置。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "mango.job.native")
public class MangoNativeJobProperties {

    /**
     * 是否启用原生调度扫描。
     */
    private boolean schedulerEnabled = true;

    /**
     * 调度扫描间隔毫秒。
     */
    private long scanIntervalMillis = 5000L;

    /**
     * 调度线程使用的租户上下文。多租户全量调度由后续租户枚举能力扩展。
     */
    private String schedulerTenantId = "1";

    /**
     * 单次扫描最大任务数。
     */
    private int scanLimit = 50;

    /**
     * 内嵌 Worker 是否启用。
     */
    private boolean embeddedWorkerEnabled = true;

    /**
     * 默认通信方式。
     */
    private JobTransportType transport = JobTransportType.IN_MEMORY;

    /**
     * 执行租约秒数。
     */
    private long leaseSeconds = 300L;

    /**
     * 当前运行环境编码。
     */
    private String envCode = "local";

    /**
     * 当前 Worker 对外执行地址。远程 Worker 部署时使用 http(s):// 地址。
     */
    private String workerAddress;

    /**
     * Worker 注册目标 JobCenter 地址。为空时不自动向远程 JobCenter 注册。
     */
    private String jobCenterAddress;

    /**
     * Worker 心跳注册间隔毫秒。
     */
    private long workerHeartbeatIntervalMillis = 15000L;
}
