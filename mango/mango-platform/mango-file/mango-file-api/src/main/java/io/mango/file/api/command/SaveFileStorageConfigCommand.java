package io.mango.file.api.command;

import io.mango.file.api.enums.FileStorageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 保存文件存储配置命令。
 */
@Data
@Schema(description = "保存文件存储配置命令")
public class SaveFileStorageConfigCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配置ID。新增时为空，修改时必填")
    private Long id;

    @NotBlank(message = "配置名称不能为空")
    @Size(max = 64, message = "配置名称不能超过64个字符")
    @Schema(description = "配置名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String configName;

    @NotNull(message = "存储类型不能为空")
    @Schema(description = "存储类型：LOCAL、S3、MINIO、AWS_S3、ALIYUN_OSS、TENCENT_COS、QINIU_KODO", requiredMode = Schema.RequiredMode.REQUIRED)
    private FileStorageType storageType;

    @Schema(description = "接入地址。本地存储可为空，S3兼容/MinIO/云厂商按实际 endpoint 填写")
    private String endpoint;

    @Schema(description = "公开访问地址。用于生成预览或公开访问地址，未配置时使用接入地址")
    private String publicEndpoint;

    @Schema(description = "区域。例如 cn-hangzhou、ap-guangzhou、us-east-1")
    private String region;

    @NotBlank(message = "存储桶不能为空")
    @Size(max = 128, message = "存储桶不能超过128个字符")
    @Schema(description = "存储桶名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bucketName;

    @Size(max = 255, message = "存储路径不能超过255个字符")
    @Schema(description = "存储路径前缀。用于隔离对象落点，例如 prod/files 或 tenant-assets")
    private String storagePath;

    @Schema(description = "访问密钥 AccessKey。本地存储可为空")
    private String accessKey;

    @Schema(description = "访问密钥 SecretKey。修改时为空表示不变")
    private String secretKey;

    @Schema(description = "是否使用 Path Style 访问。MinIO 和多数私有 S3 兼容服务通常需要开启")
    private Boolean pathStyleAccess;

    @Schema(description = "是否启用 HTTPS")
    private Boolean sslEnabled;

    @Schema(description = "是否设为默认启用配置")
    private Boolean active;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
