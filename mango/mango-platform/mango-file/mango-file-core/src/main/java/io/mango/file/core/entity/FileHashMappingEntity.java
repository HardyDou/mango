package io.mango.file.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件秒传哈希映射。
 */
@Data
@TableName("file_hash_mapping")
public class FileHashMappingEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String scopeType;
    private Long tenantId;
    private Long storageConfigId;
    private String fileHash;
    private Long fileSize;
    private Long objectId;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private LocalDateTime updatedAt;
}
