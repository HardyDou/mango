package io.mango.file.core.config;

import io.mango.file.api.enums.FileStorageType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 文件能力配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.file")
public class FileProperties {

    /** 是否启用文件能力。 */
    private boolean enabled = true;

    /** 默认存储类型。 */
    private FileStorageType storageType = FileStorageType.LOCAL;

    /** 默认本地存储桶。 */
    private String defaultBucket = "local";

    /** 文件代理访问外部基准地址，例如 https://example.com/api。 */
    private String publicBaseUrl;

    /** 本地存储配置。 */
    private Local local = new Local();

    /** 上传限制配置。 */
    private Upload upload = new Upload();

    /** 访问控制默认配置。 */
    private Access access = new Access();

    /** 预览默认配置。 */
    private Preview preview = new Preview();

    @Data
    public static class Local {

        /** 本地存储根目录。 */
        private String rootPath = "./data/files";

        /** 本地对象客户端访问路径。 */
        private String publicPath = "/api/file/local-objects";
    }

    @Data
    public static class Upload {

        /** 单文件最大大小，单位字节。 */
        private long maxSize = 100L * 1024L * 1024L;

        /** 允许的扩展名。为空表示不限制。 */
        private List<String> allowedExtensions = List.of();

        /** 禁止的扩展名。 */
        private List<String> blockedExtensions = List.of("exe", "bat", "cmd", "sh", "jar");

        /** 是否启用秒传。 */
        private boolean instantUploadEnabled = true;

        /** 是否允许浏览器直传对象存储。 */
        private boolean directUploadEnabled = false;

        /** 直传 URL 有效期，单位秒。 */
        private long directUploadExpireSeconds = 900L;
    }

    @Data
    public static class Access {

        /** 文件访问模式：PROXY-通过 Java 服务转发，DIRECT-直连底层存储。 */
        private String mode = "PROXY";

        /** 是否启用带时效的访问令牌。 */
        private boolean tokenEnabled = false;

        /** 下载/访问令牌有效期，单位秒。 */
        private long tokenExpireSeconds = 600L;
    }

    @Data
    public static class Preview {

        /** 文档预览服务地址。支持绝对地址、相对地址和 {fileId}/{fileUrl}/{fileName}/{expireSeconds} 占位符。 */
        private String providerUrl = "/file-preview/files/preview";

        /** 文档预览访问有效期，单位秒。 */
        private long expireSeconds = 600L;

        /** 可交由文档预览服务处理的扩展名。为空表示所有文件都可进入预览服务。 */
        private List<String> externalExtensions = List.of(
                "doc", "docx", "xls", "xlsx", "xlsm", "ppt", "pptx",
                "odt", "ods", "odp", "ofd", "wps", "et", "dps",
                "csv", "txt", "zip", "rar", "7z", "eml", "msg"
        );
    }
}
