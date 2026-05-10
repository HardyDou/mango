package io.mango.file.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件记录实体。
 */
@Data
@TableName("sys_file_record")
public class FileRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String bizType;
    private String bizId;
    private String purpose;
    private String accessLevel;
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
    private Long createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private LocalDateTime updatedAt;
}
