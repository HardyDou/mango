package io.mango.file.api.vo;

import io.mango.file.api.enums.FileStorageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件存储配置。
 */
@Data
@Schema(description = "文件存储配置")
public class FileStorageConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "存储类型：LOCAL、S3、MINIO、AWS_S3、ALIYUN_OSS、TENCENT_COS、QINIU_KODO")
    private FileStorageType storageType;

    @Schema(description = "接入地址")
    private String endpoint;

    @Schema(description = "公开访问地址")
    private String publicEndpoint;

    @Schema(description = "区域")
    private String region;

    @Schema(description = "存储桶名称")
    private String bucketName;

    @Schema(description = "存储路径前缀")
    private String storagePath;

    @Schema(description = "访问密钥 AccessKey")
    private String accessKey;

    @Schema(description = "是否已配置 SecretKey")
    private Boolean secretConfigured;

    @Schema(description = "是否使用 Path Style 访问")
    private Boolean pathStyleAccess;

    @Schema(description = "是否启用 HTTPS")
    private Boolean sslEnabled;

    @Schema(description = "是否默认启用")
    private Boolean active;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
