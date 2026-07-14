package io.mango.cms.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveCmsContentCategoryCommand {

    @Schema(description = "主键 ID")
    @Positive(message = "主键 ID 必须大于 0")
    private Long id;

    @Schema(description = "父级 ID")
    @PositiveOrZero(message = "父级 ID 不能小于 0")
    private Long parentId;

    @NotBlank(message = "分类编码不能为空")
    @Size(max = 64, message = "分类编码最多64个字符")
    @Pattern(regexp = "[A-Za-z0-9_.:-]+", message = "分类编码只能包含字母、数字、点、下划线、冒号和短横线")
    @Schema(description = "分类编码")
    private String categoryCode;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 128, message = "分类名称最多128个字符")
    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "排序值")
    @PositiveOrZero(message = "排序值不能小于 0")
    private Integer sort;

    @Pattern(regexp = "ENABLED|DISABLED", message = "状态不合法")
    @Schema(description = "状态")
    private String status;

    @Size(max = 512, message = "备注最多512个字符")
    @Schema(description = "备注")
    private String remark;
}
