package io.mango.resource.api.command;

import io.mango.resource.api.enums.ResourceSyncMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 后台新增托管资源命令。
 */
@Data
@Schema(description = "后台新增托管资源命令")
public class CreateResourceRegistryCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "资源类型不能为空")
    @Schema(description = "资源类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String resourceType;

    @NotBlank(message = "模块编码不能为空")
    @Schema(description = "模块编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String moduleCode;

    @NotBlank(message = "业务键不能为空")
    @Schema(description = "业务稳定键", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bizKey;

    @NotBlank(message = "资源名称不能为空")
    @Schema(description = "资源名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "目标模块不能为空")
    @Schema(description = "目标模块", requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetModule;

    @NotNull(message = "同步模式不能为空")
    @Schema(description = "同步模式，默认 MANUAL 表示后台托管", requiredMode = Schema.RequiredMode.REQUIRED)
    private ResourceSyncMode syncMode = ResourceSyncMode.MANUAL;

    @NotBlank(message = "资源字段JSON不能为空")
    @Schema(description = "资源字段JSON对象，结构与资源声明 fields 一致", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fields = "{}";
}
