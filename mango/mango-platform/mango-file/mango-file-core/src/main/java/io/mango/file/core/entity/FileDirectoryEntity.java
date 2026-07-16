package io.mango.file.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件逻辑目录实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_directory")
public class FileDirectoryEntity extends FileTenantEntity {

    private Long parentId;
    private String directoryName;
    private String directoryPath;
    private Integer sort;
    private Integer status;
}
