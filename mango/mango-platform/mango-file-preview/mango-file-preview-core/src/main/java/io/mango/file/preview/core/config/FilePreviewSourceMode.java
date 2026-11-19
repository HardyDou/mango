package io.mango.file.preview.core.config;

/**
 * 预览引擎获取源文件的方式。
 */
public enum FilePreviewSourceMode {

    /** 始终让引擎用 HTTP 抓取源文件地址，兼容独立部署的引擎。 */
    URL,

    /** 始终走进程内直取（{@code fileid://}），要求引擎与文件服务同进程。 */
    FILE_ID,

    /** 同进程（内嵌引擎）走直取，独立部署的引擎走 HTTP；默认值。 */
    AUTO
}
