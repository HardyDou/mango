package io.mango.file.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 物理文件对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_object")
public class FileObjectEntity extends FileTenantEntity {

    private Long storageConfigId;
    private String storageType;
    private String bucketName;
    private String objectName;
    private String fileHash;
    private Long fileSize;
    private String contentType;
    private Integer status;
    private Long refCount;
}
