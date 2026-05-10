package io.mango.file.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件记录。
 */
@Data
@Schema(description = "文件记录")
public class FileRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文件ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long tenantId;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务ID")
    private String bizId;

    @Schema(description = "文件用途")
    private String purpose;

    @Schema(description = "访问级别：PRIVATE、PUBLIC_READ、INTERNAL")
    private String accessLevel;

    @Schema(description = "存储类型：LOCAL、S3、MINIO、AWS_S3、ALIYUN_OSS、TENCENT_COS、QINIU_KODO")
    private String storageType;

    @Schema(description = "存储配置ID")
    private Long storageConfigId;

    @Schema(description = "存储桶名称")
    private String bucketName;

    @Schema(description = "对象名称")
    private String objectName;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件扩展名")
    private String fileExt;

    @Schema(description = "文件大小，单位字节")
    private Long fileSize;

    @Schema(description = "内容类型")
    private String contentType;

    @Schema(description = "文件哈希")
    private String fileHash;

    @Schema(description = "状态：0-上传中，1-完成，2-失败，9-归档")
    private Integer status;

    @Schema(description = "是否已归档")
    private Integer archived;

    @Schema(description = "创建人ID")
    private Long createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
