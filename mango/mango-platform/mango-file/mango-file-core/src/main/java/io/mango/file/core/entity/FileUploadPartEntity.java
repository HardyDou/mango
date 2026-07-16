package io.mango.file.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件分片上传明细。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_upload_part")
public class FileUploadPartEntity extends FileTenantEntity {

    private Long sessionId;
    private Integer partNumber;
    private Long partSize;
    private String partHash;
    private String etag;
    private String status;
}
