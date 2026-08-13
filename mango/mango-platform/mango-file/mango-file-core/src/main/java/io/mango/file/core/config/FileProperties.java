package io.mango.file.core.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.file.api.enums.FileStorageType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 文件能力配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.file")
public class FileProperties {

    private static final long DEFAULT_ACCESS_EXPIRE_SECONDS = 86400L;
    private static final long DEFAULT_CONNECT_TIMEOUT_MILLIS = 3000L;
    private static final long DEFAULT_READ_TIMEOUT_MILLIS = 10000L;
    private static final long DEFAULT_UPLOAD_EXPIRE_SECONDS = 900L;
    private static final long DEFAULT_UPLOAD_MAX_SIZE = 100L * 1024L * 1024L;
    private static final long DEFAULT_MULTIPART_THRESHOLD = 20L * 1024L * 1024L;
    private static final long DEFAULT_REMOTE_IMAGE_MAX_SIZE = 10L * 1024L * 1024L;
    private static final int DEFAULT_MAX_REDIRECTS = 3;
    private static final int HTTP_PORT = 80;
    private static final int HTTPS_PORT = 443;

    /** 是否启用文件能力。 */
    private boolean enabled = true;

    /** 默认存储类型。 */
    private FileStorageType storageType = FileStorageType.LOCAL;

    /** 默认本地存储桶。 */
    private String defaultBucket = "local";

    /** 文件代理访问外部基准地址，例如 https://example.com/api。 */
    private String publicBaseUrl;

    /** FILE_ASSET 的外部资产根目录；仅在声明使用 asset: 协议时读取。 */
    private String assetRoot;

    /** 本地存储配置。 */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Nested Spring configuration is exposed for property binding"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Nested Spring configuration is accepted during property binding"))
    private Local local = new Local();

    /** 上传限制配置。 */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Nested Spring configuration is exposed for property binding"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Nested Spring configuration is accepted during property binding"))
    private Upload upload = new Upload();

    /** 访问控制默认配置。 */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Nested Spring configuration is exposed for property binding"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Nested Spring configuration is accepted during property binding"))
    private Access access = new Access();

    /** 预览默认配置。 */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Nested Spring configuration is exposed for property binding"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Nested Spring configuration is accepted during property binding"))
    private Preview preview = new Preview();

    /** 远程图片导入配置。 */
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Nested Spring configuration is exposed for property binding"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Nested Spring configuration is accepted during property binding"))
    private RemoteImport remoteImport = new RemoteImport();

    @Data
    public static class Local {

        /** 本地存储根目录。 */
        private String rootPath = "./data/files";

        /** 本地对象 Java 服务访问路径。 */
        private String publicPath = "/file/local-objects";
    }

    @Data
    public static class Upload {

        /** 单文件最大大小，单位字节。 */
        private long maxSize = DEFAULT_UPLOAD_MAX_SIZE;

        /** 允许的扩展名。为空表示不限制。 */
        @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
                justification = "Spring configuration list is exposed for property binding"))
        @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
                justification = "Spring configuration list is accepted during property binding"))
        private List<String> allowedExtensions = List.of();

        /** 禁止的扩展名。 */
        @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
                justification = "Spring configuration list is exposed for property binding"))
        @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
                justification = "Spring configuration list is accepted during property binding"))
        private List<String> blockedExtensions = List.of("exe", "bat", "cmd", "sh", "jar");

        /** 是否启用秒传。 */
        private boolean instantUploadEnabled = true;

        /** 是否启用大文件分片上传。 */
        private boolean multipartEnabled = true;

        /** 大文件分片上传临界值，单位字节。 */
        private long multipartThreshold = DEFAULT_MULTIPART_THRESHOLD;

        /** 是否允许浏览器直传对象存储。 */
        private boolean directUploadEnabled = false;

        /** 直传 URL 有效期，单位秒。 */
        private long directUploadExpireSeconds = DEFAULT_UPLOAD_EXPIRE_SECONDS;
    }

    @Data
    public static class Access {

        /** 文件访问模式：PROXY-通过 Java 服务转发，DIRECT-使用存储公开访问地址。 */
        private String mode = "DIRECT";

        /** 是否启用带时效的访问令牌。 */
        private boolean tokenEnabled = true;

        /** 下载/访问令牌有效期，单位秒。 */
        private long tokenExpireSeconds = DEFAULT_ACCESS_EXPIRE_SECONDS;
    }

    @Data
    public static class Preview {

        /** 文档预览服务地址。支持绝对地址、相对地址和 {fileId}/{fileUrl}/{fileName}/{expireSeconds} 占位符。 */
        private String providerUrl = "/file-preview/files/preview";

        /** 文档预览访问有效期，单位秒。 */
        private long expireSeconds = DEFAULT_ACCESS_EXPIRE_SECONDS;

        /** 可交由文档预览服务处理的扩展名。为空表示所有文件都可进入预览服务。 */
        @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
                justification = "Spring configuration list is exposed for property binding"))
        @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
                justification = "Spring configuration list is accepted during property binding"))
        private List<String> externalExtensions = List.of(
                "doc", "docx", "xls", "xlsx", "xlsm", "ppt", "pptx",
                "odt", "ods", "odp", "ofd", "wps", "et", "dps",
                "csv", "txt", "zip", "rar", "7z", "eml", "msg"
        );
    }

    @Data
    public static class RemoteImport {

        /** 是否启用受控远程图片导入。 */
        private boolean enabled = true;

        /** 连接超时，单位毫秒。 */
        private long connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS;

        /** 单次请求总读取超时，单位毫秒。 */
        private long readTimeoutMillis = DEFAULT_READ_TIMEOUT_MILLIS;

        /** 最大响应字节数。 */
        private long maxSize = DEFAULT_REMOTE_IMAGE_MAX_SIZE;

        /** 最大重定向跳数。 */
        private int maxRedirects = DEFAULT_MAX_REDIRECTS;

        /** 允许连接的端口。 */
        @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
                justification = "Spring configuration list is exposed for property binding"))
        @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
                justification = "Spring configuration list is accepted during property binding"))
        private List<Integer> allowedPorts = List.of(HTTP_PORT, HTTPS_PORT);
    }
}
