package io.mango.resource.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 资源目标模块内部执行命令。
 */
@Data
public class ExecuteResourceTargetCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "资源声明JSON不能为空")
    @Schema(description = "资源声明JSON数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private String declarations;

    @NotNull(message = "完整批次JSON不能为空")
    @Schema(description = "同类型完整有效资源批次JSON数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private String completeBatch;
}
