package io.mango.job.api.enums;

/**
 * JobCenter 与 Worker 通信方式。
 */
public enum JobTransportType {

    /**
     * 同进程内存通信，用于单体内嵌部署，不开放 Worker 独立端口。
     */
    IN_MEMORY,

    /**
     * Mango 内部 HTTP 通信，用于独立 JobCenter 与远程 Worker。
     */
    HTTP_INTERNAL
}
