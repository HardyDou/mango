package io.mango.file.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件秒传哈希映射。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_hash_mapping")
public class FileHashMappingEntity extends FileTenantEntity {

    private String scopeType;
    private Long storageConfigId;
    private String fileHash;
    private Long fileSize;
    private Long objectId;
    private Integer status;
}
