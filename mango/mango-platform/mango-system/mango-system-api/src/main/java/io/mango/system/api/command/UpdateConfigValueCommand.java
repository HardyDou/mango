package io.mango.system.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "系统配置值修改命令")
public class UpdateConfigValueCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配置ID")
    @NotNull(message = "配置ID不能为空")
    private Long id;

    @Schema(description = "配置值")
    private String value;
}
