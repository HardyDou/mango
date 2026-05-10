package io.mango.file.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件存储配置实体。
 */
@Data
@TableName("sys_file_storage_config")
public class FileStorageConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String configName;
    private String storageType;
    private String endpoint;
    private String publicEndpoint;
    private String region;
    private String bucketName;
    private String accessKey;
    private String secretKey;
    private Integer pathStyleAccess;
    private Integer sslEnabled;
    private Integer active;
    private Integer status;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private LocalDateTime updatedAt;
}
