package io.mango.link.api.command;

import io.mango.link.api.enums.LinkVisibilityTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 网址可见目标配置。
 */
@Data
@Schema(description = "网址可见目标配置")
public class LinkVisibilityTargetCommand {

    @NotNull(message = "目标类型不能为空")
    @Schema(description = "目标类型：DEPARTMENT、USER")
    private LinkVisibilityTargetType targetType;

    @NotNull(message = "目标 ID 不能为空")
    @Schema(description = "部门 ID 或用户 ID")
    private Long targetId;

    @Size(max = 128, message = "目标名称最多128个字符")
    @Schema(description = "部门名或用户显示名快照")
    private String targetName;
}
