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
     * 预览源文件是否在同一进程内直取。
     *
     * <p>内嵌引擎（默认部署）保持 {@code true}：预览地址传 {@code fileId} 参数，引擎向本进程取流。
     * 独立进程部署的引擎（{@code ServerMain}，见 {@code mango.file-preview.engine.port}）必须置
     * {@code false}：那个进程里没有本应用的取源实现，传 {@code fileId} 无法解析，必须回退为
     * {@code url} 参数经 {@code source-base-url} 取流。</p>
     */
    private boolean internalSourceEnabled = true;

    /** 预览引擎入口路径。 */
    private String enginePath = "/onlinePreview";

    /** 源文件临时访问路径。 */
    private String sourcePath = "/file-preview/sources";

    /** 预览引擎访问源文件时使用的内部服务地址；为空时使用当前请求地址。 */
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
