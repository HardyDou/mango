package io.mango.template.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 修改模板命令。
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(description = "修改模板命令")
public class UpdateTemplateCommand extends SaveTemplateCommand {

    @NotNull(message = "模板ID不能为空")
    @Positive(message = "模板ID必须为正数")
    @Schema(description = "模板ID")
    private Long id;
}
