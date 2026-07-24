package io.mango.file.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件中心运行时配置实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_settings")
public class FileSettingsEntity extends FileTenantEntity {

    private Long maxSize;
    private String allowedExtensions;
    private String blockedExtensions;
    private String defaultAccessLevel;
    private String duplicateNameStrategy;
    private Integer duplicateCheckDirectoryScoped;
    private String objectNameStrategy;
    private Integer instantUploadEnabled;
    private Integer multipartEnabled;
    private Long multipartThreshold;
    private String instantUploadScope;
    private Integer contentTypeCheckEnabled;
    private String allowedContentTypes;
    private String blockedContentTypes;
    private Integer directUploadEnabled;
    private Long directUploadExpireSeconds;
    private Integer accessTokenEnabled;
    private Integer publicReadRequiresToken;
    private String accessMode;
    private Long accessTokenExpireSeconds;
    private String previewProviderUrl;
    private Long previewExpireSeconds;
    private String previewExternalExtensions;
    private Integer archiveRetainEnabled;
    private Integer archiveRetainDays;
    private Integer archiveRestoreEnabled;
    private Integer physicalDeleteEnabled;
}
