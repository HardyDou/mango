package io.mango.link.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新个人网址分组命令。
 */
@Data
@Schema(description = "更新个人网址分组命令")
public class UpdateLinkPersonalCategoryCommand {

    @NotNull(message = "分组 ID 不能为空")
    @Schema(description = "分组 ID")
    private Long id;

    @NotBlank(message = "分组名称不能为空")
    @Size(max = 64, message = "分组名称最多64个字符")
    @Schema(description = "分组名称")
    private String name;

    @Min(value = 0, message = "排序号不能小于0")
    @Max(value = 999999, message = "排序号不能大于999999")
    @Schema(description = "排序号")
    private Integer sortNo;
}
