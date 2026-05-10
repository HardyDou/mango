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

    /** 本地存储配置。 */
    private Local local = new Local();

    /** 上传限制配置。 */
    private Upload upload = new Upload();

    @Data
    public static class Local {

        /** 本地存储根目录。 */
        private String rootPath = "./data/files";
    }

    @Data
    public static class Upload {

        /** 单文件最大大小，单位字节。 */
        private long maxSize = 100L * 1024L * 1024L;

        /** 允许的扩展名。为空表示不限制。 */
        private List<String> allowedExtensions = List.of();

        /** 禁止的扩展名。 */
        private List<String> blockedExtensions = List.of("exe", "bat", "cmd", "sh", "jar");
    }
}
