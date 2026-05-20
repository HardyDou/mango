package io.mango.file.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物理文件对象。
 */
@Data
@TableName("file_object")
public class FileObjectEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long storageConfigId;
    private String storageType;
    private String bucketName;
    private String objectName;
    private String fileHash;
    private Long fileSize;
    private String contentType;
    private Integer status;
    private Long refCount;
    private Long createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private LocalDateTime updatedAt;
}
