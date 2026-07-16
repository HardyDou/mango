package io.mango.file.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件记录实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_record")
public class FileRecordEntity extends FileTenantEntity {

    private String bizType;
    private String bizId;
    private String purpose;
    private String bizMeta;
    private Long directoryId;
    private String accessLevel;
    private Long objectId;
    private String storageType;
    private Long storageConfigId;
    private String bucketName;
    private String objectName;
    private String fileName;
    private String fileExt;
    private Long fileSize;
    private String contentType;
    private String fileHash;
    private Integer status;
    private Integer archived;
}
