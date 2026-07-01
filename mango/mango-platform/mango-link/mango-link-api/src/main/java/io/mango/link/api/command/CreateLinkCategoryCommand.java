package io.mango.link.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增网址分类命令。
 */
@Data
@Schema(description = "新增网址分类命令")
public class CreateLinkCategoryCommand {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称最多64个字符")
    @Schema(description = "分类名称")
    private String name;

    @Min(value = 0, message = "排序号不能小于0")
    @Max(value = 999999, message = "排序号不能大于999999")
    @Schema(description = "排序号")
    private Integer sortNo;

    @Size(max = 256, message = "备注最多256个字符")
    @Schema(description = "后台备注")
    private String remark;
}
