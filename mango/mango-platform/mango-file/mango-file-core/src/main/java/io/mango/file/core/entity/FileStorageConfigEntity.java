package io.mango.file.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件存储配置实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_storage_config")
public class FileStorageConfigEntity extends FileTenantEntity {

    private String configName;
    private String storageType;
    private String endpoint;
    private String publicEndpoint;
    private String region;
    private String bucketName;
    private String storagePath;
    private String accessKey;
    private String secretKey;
    private Integer pathStyleAccess;
    private Integer sslEnabled;
    private Integer active;
    private Integer status;
    private String remark;
}
