package io.mango.cms.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCmsStatusCommand {

    @NotNull(message = "ID 不能为空")
    @Schema(description = "主键 ID")
    private Long id;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "ENABLED|DISABLED", message = "状态不合法")
    @Schema(description = "状态")
    private String status;
}
