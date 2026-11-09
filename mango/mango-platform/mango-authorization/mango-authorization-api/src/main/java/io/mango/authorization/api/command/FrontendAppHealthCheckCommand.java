package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 前端运行单元健康检查命令。
 */
@Data
@Schema(description = "前端运行单元健康检查命令")
public class FrontendAppHealthCheckCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "运行单元编码不能为空")
    @Size(max = 64, message = "运行单元编码最多64个字符")
    @Schema(description = "前端运行单元编码，对应注册表 appCode")
    private String runtimeCode;
}
