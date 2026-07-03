package io.mango.home.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "更新首页模板草稿命令")
public class UpdateHomeTemplateDraftCommand implements Serializable {

    @NotNull(message = "模板ID不能为空")
    @Schema(description = "模板ID")
    private Long id;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 64, message = "模板名称长度不能超过64")
    @Schema(description = "模板名称")
    private String name;

    @NotBlank(message = "草稿布局不能为空")
    @Schema(description = "草稿布局JSON")
    private String layoutJson;
}
