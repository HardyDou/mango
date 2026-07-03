package io.mango.link.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 取消收藏网址命令。
 */
@Data
@Schema(description = "取消收藏网址命令")
public class DeleteLinkFavoriteCommand {

    @NotNull(message = "网址 ID 不能为空")
    @Schema(description = "网址 ID")
    private Long linkId;
}
