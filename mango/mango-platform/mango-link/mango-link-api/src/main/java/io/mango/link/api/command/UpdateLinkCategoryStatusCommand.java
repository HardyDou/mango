package io.mango.link.api.command;

import io.mango.link.api.enums.LinkStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新网址分类状态命令。
 */
@Data
@Schema(description = "更新网址分类状态命令")
public class UpdateLinkCategoryStatusCommand {

    @NotNull(message = "分类 ID 不能为空")
    @Schema(description = "分类 ID")
    private Long id;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：ENABLED、DISABLED")
    private LinkStatus status;
}
