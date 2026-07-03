package io.mango.home.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "更新首页模板状态命令")
public class UpdateHomeTemplateStatusCommand implements Serializable {

    @NotNull(message = "模板ID不能为空")
    @Schema(description = "模板ID")
    private Long id;

    @NotNull(message = "启用状态不能为空")
    @Schema(description = "是否启用")
    private Boolean enabled;
}
