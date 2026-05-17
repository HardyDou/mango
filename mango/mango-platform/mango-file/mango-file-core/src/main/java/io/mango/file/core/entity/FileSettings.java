package io.mango.file.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件中心运行时配置实体。
 */
@Data
@TableName("file_settings")
public class FileSettings {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long maxSize;
    private String allowedExtensions;
    private String blockedExtensions;
    private String defaultAccessLevel;
    private String duplicateNameStrategy;
    private Integer duplicateCheckDirectoryScoped;
    private String objectNameStrategy;
    private Integer instantUploadEnabled;
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
    private Long createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private LocalDateTime updatedAt;
}
