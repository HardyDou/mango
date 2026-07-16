package io.mango.file.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 文件分片上传会话。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_upload_session")
public class FileUploadSessionEntity extends FileTenantEntity {

    private Long storageConfigId;
    private String storageType;
    private String bucketName;
    private String objectName;
    private String uploadMode;
    private String storageUploadId;
    private String fileName;
    private String fileExt;
    private String fileHash;
    private Long fileSize;
    private String contentType;
    private Long chunkSize;
    private Integer totalParts;
    private Integer uploadedParts;
    private String status;
    private LocalDateTime expiresAt;
    private String purpose;
    private String accessLevel;
    private String bizType;
    private String bizId;
    private String bizMeta;
    private Long directoryId;
    private Long objectId;
    private Long fileRecordId;
}
