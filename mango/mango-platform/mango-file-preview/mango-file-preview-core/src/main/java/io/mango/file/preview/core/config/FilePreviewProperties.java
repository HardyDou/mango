package io.mango.file.preview.core.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件预览配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.file-preview")
public class FilePreviewProperties {

    private static final long DEFAULT_SOURCE_TOKEN_EXPIRE_SECONDS = 86_400L;

    /** 是否启用文件预览。 */
    private boolean enabled = true;

    /**
     * 运维开关：预览源文件是否允许在本进程内直取。
     *
     * <p>{@code true}（默认）时，只要装配了取源实现（内嵌部署的 {@code FilePreviewEngineDownloader}
     * 或独立引擎的远程直取实现），预览地址就传 {@code fileId} 参数，引擎按短期票据取流；
     * 置为 {@code false} 表示**强制**回退到 {@code url} 通道（此时必须配置
     * {@link #sourceBaseUrl}，缺失会报 {@code 180004}），仅用于排查问题或保留旧行为。</p>
     */
    private boolean internalSourceEnabled = true;

    /** 预览引擎入口路径。 */
    private String enginePath = "/onlinePreview";

    /**
     * 预览入口地址（浏览器侧相对路径），如 {@code /api/file-preview/files/preview-entry}。
     *
     * <p>该地址由本应用提供，但通常经前端代理访问，所以默认带 {@code /api} 前缀；
     * 直连后端、或前端代理前缀不是 {@code /api} 的部署必须覆盖本项，否则返回的
     * {@code previewUrl} 会 404。可以写成完整 URL（{@code https://app.example.com/file-preview/...}），
     * 用于前端与应用不同源的部署。</p>
     */
    private String entryPath = "/api/file-preview/files/preview-entry";

    /**
     * 引擎的对外访问地址（浏览器可达），如 {@code https://engine.example.com}。
     *
     * <p>留空表示引擎与应用同源（内嵌部署），此时返回相对地址。
     * 与 {@code mango.file-preview.remote.base-url}（引擎回打应用的地址）语义不同，不可互相替代。</p>
     */
    private String enginePublicBaseUrl;

    /** 源文件临时访问路径。 */
    private String sourcePath = "/file-preview/sources";

    /**
     * {@code url} 兜底通道读取源文件时使用的内部服务地址；只在没有取源实现可用时使用。
     *
     * <p>该配置不再从当前请求地址推断：走 {@code url} 兜底但未配置时直接报 {@code 180004}，
     * 避免在网关/反代后静默生成一个引擎访问不到的地址。</p>
     */
    private String sourceBaseUrl;


    /** 源文件临时访问令牌有效期，单位秒。 */
    private long sourceTokenExpireSeconds = DEFAULT_SOURCE_TOKEN_EXPIRE_SECONDS;

    /** 是否允许访问 kkFileView 独立首页与演示文件管理入口。 */
    private boolean standaloneUiEnabled = false;

    /** 转换调度配置。 */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring configuration object is managed by the container"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring configuration object is managed by the container"))
    private Conversion conversion = new Conversion();

    @Data
    public static class Conversion {

        private static final int DEFAULT_WORKER_COUNT = 2;
        private static final int DEFAULT_OFFICE_BASE_PORT = 2001;

        /** 每个预览实例的转换 Worker 数量。 */
        private int workerCount = 2;

        /** 未显式配置端口列表时生成 LibreOffice 端口的起始端口。 */
        private int officeBasePort = DEFAULT_OFFICE_BASE_PORT;

        public int effectiveWorkerCount() {
            return workerCount > 0 ? workerCount : DEFAULT_WORKER_COUNT;
        }
    }
}
