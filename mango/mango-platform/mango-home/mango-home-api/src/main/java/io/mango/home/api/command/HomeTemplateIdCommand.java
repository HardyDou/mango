package io.mango.home.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "首页模板ID命令")
public class HomeTemplateIdCommand implements Serializable {

    @NotNull(message = "模板ID不能为空")
    @Schema(description = "模板ID")
    private Long id;
}
