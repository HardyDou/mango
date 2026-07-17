package io.mango.file.preview.core.config;

import lombok.Data;
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
}
