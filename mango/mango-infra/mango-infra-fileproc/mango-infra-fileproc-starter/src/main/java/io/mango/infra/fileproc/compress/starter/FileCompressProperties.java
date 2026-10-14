package io.mango.infra.fileproc.compress.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件压缩工具配置。
 */
@ConfigurationProperties(prefix = "mango.fileproc.compress")
public class FileCompressProperties {

    private static final long DEFAULT_TIMEOUT_SECONDS = 900L;

    /**
     * 是否启用文件压缩自动配置。
     */
    private boolean enabled = true;

    /**
     * Ghostscript 可执行文件路径；未配置时使用系统 PATH 中的 gs。
     */
    private String ghostscriptPath = "gs";

    /**
     * 单次 Ghostscript 压缩超时时间，单位秒。
     */
    private long timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getGhostscriptPath() {
        return ghostscriptPath;
    }

    public void setGhostscriptPath(String ghostscriptPath) {
        this.ghostscriptPath = ghostscriptPath;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
