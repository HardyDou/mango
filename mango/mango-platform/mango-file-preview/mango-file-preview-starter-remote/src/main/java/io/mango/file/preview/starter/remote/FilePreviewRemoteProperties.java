package io.mango.file.preview.starter.remote;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 预览引擎远程取源配置。
 *
 * <p>只用于独立部署的预览引擎：引擎进程按票据回 Mango 主应用直取源文件，
 * 因此需要 Mango 的对外/内网地址与独立的超时预算（大文件取源比普通 JSON 调用慢得多）。</p>
 */
@ConfigurationProperties(prefix = "mango.file-preview.remote")
public class FilePreviewRemoteProperties {

    /** 默认连接超时。 */
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);

    /** 默认读超时：视频等大文件取源需要更长的读窗口。 */
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(60);

    /** 是否启用远程直取取源。 */
    private boolean enabled;

    /** Mango 主应用的对外/内网地址，例如 {@code http://mango-app:8080}。 */
    private String baseUrl;

    /** 连接超时。 */
    private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    /** 读超时。 */
    private Duration readTimeout = DEFAULT_READ_TIMEOUT;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout == null ? DEFAULT_CONNECT_TIMEOUT : connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout == null ? DEFAULT_READ_TIMEOUT : readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
}
