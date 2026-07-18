package io.mango.file.api.query;

import io.mango.common.po.PageQuery;
import io.mango.file.api.enums.FileStorageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件存储配置分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文件存储配置分页查询")
public class FileStorageConfigPageQuery extends PageQuery {

    @Schema(description = "关键词。支持配置名称、存储桶、接入地址模糊搜索")
    @Size(max = 255, message = "关键词不能超过255个字符")
    private String keyword;

    @Schema(description = "存储类型：LOCAL、S3、MINIO、AWS_S3、ALIYUN_OSS、TENCENT_COS、QINIU_KODO")
    private FileStorageType storageType;

    @Schema(description = "是否默认启用")
    private Boolean active;

    @Schema(description = "状态：1-启用，0-停用")
    @Min(value = 0, message = "状态值不能小于0")
    @Max(value = 1, message = "状态值不能大于1")
    private Integer status;
}
