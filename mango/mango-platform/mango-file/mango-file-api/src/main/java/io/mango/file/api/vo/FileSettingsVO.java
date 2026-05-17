package io.mango.file.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件中心运行时配置。
 */
@Data
@Schema(description = "文件中心运行时配置")
public class FileSettingsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long tenantId;

    @Schema(description = "单文件最大大小，单位字节")
    private Long maxSize;

    @Schema(description = "允许上传的扩展名。为空表示不限制")
    private List<String> allowedExtensions;

    @Schema(description = "禁止上传的扩展名")
    private List<String> blockedExtensions;

    @Schema(description = "默认访问级别：PRIVATE、PUBLIC_READ、INTERNAL")
    private String defaultAccessLevel;

    @Schema(description = "文件重名处理策略：REJECT-拒绝、AUTO_RENAME-自动重命名、ALLOW-允许重复")
    private String duplicateNameStrategy;

    @Schema(description = "是否按逻辑目录隔离重名")
    private Boolean duplicateCheckDirectoryScoped;

    @Schema(description = "对象命名策略：DATE_UUID、HASH、ORIGINAL")
    private String objectNameStrategy;

    @Schema(description = "是否启用秒传")
    private Boolean instantUploadEnabled;

    @Schema(description = "秒传匹配范围：TENANT-机构内、GLOBAL-全局")
    private String instantUploadScope;

    @Schema(description = "是否校验内容类型")
    private Boolean contentTypeCheckEnabled;

    @Schema(description = "允许上传的内容类型。为空表示不限制")
    private List<String> allowedContentTypes;

    @Schema(description = "禁止上传的内容类型")
    private List<String> blockedContentTypes;

    @Schema(description = "是否启用浏览器直传对象存储")
    private Boolean directUploadEnabled;

    @Schema(description = "直传 URL 有效期，单位秒")
    private Long directUploadExpireSeconds;

    @Schema(description = "是否启用限时访问令牌")
    private Boolean accessTokenEnabled;

    @Schema(description = "公开读取文件是否仍强制签名访问")
    private Boolean publicReadRequiresToken;

    @Schema(description = "文件访问模式：PROXY-通过Java服务转发，DIRECT-直连底层存储公开地址")
    private String accessMode;

    @Schema(description = "下载/访问令牌有效期，单位秒")
    private Long accessTokenExpireSeconds;

    @Schema(description = "文档预览服务地址")
    private String previewProviderUrl;

    @Schema(description = "文档预览访问有效期，单位秒")
    private Long previewExpireSeconds;

    @Schema(description = "可交由文档预览服务处理的扩展名")
    private List<String> previewExternalExtensions;

    @Schema(description = "是否保留归档记录")
    private Boolean archiveRetainEnabled;

    @Schema(description = "归档记录保留天数")
    private Integer archiveRetainDays;

    @Schema(description = "是否允许恢复归档")
    private Boolean archiveRestoreEnabled;

    @Schema(description = "是否删除物理对象。默认否，仅归档记录")
    private Boolean physicalDeleteEnabled;

    @Schema(description = "是否来自 yml 默认配置")
    private Boolean defaultConfig;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
