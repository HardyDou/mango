package io.mango.link.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 更新后台网址命令。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "更新后台网址命令")
public class UpdateLinkItemCommand extends CreateLinkItemCommand {

    @NotNull(message = "网址 ID 不能为空")
    @Schema(description = "网址 ID")
    private Long id;
}
