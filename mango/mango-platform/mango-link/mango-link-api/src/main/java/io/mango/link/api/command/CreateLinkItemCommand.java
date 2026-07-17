package io.mango.link.api.command;

import io.mango.link.api.enums.LinkVisibilityScope;
import io.mango.link.api.validation.LinkStrictValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新增后台网址命令。
 */
@Data
@Schema(description = "新增后台网址命令")
public class CreateLinkItemCommand {

    @NotBlank(message = "网址名称不能为空")
    @Size(max = 128, message = "网址名称最多128个字符")
    @Schema(description = "网址名称")
    private String name;

    @NotBlank(message = "网址地址不能为空")
    @Size(max = 1024, message = "网址地址最多1024个字符")
    @Schema(description = "网址地址")
    private String url;

    @NotNull(message = "网址分类不能为空")
    @Schema(description = "分类 ID")
    private Long categoryId;

    @Size(max = 256, message = "简介最多256个字符")
    @Schema(description = "简介")
    private String summary;

    @Size(max = 1024, message = "图标地址最多1024个字符")
    @Schema(description = "图标地址")
    private String iconUrl;

    @Size(max = 20, message = "标签最多20个")
    @Schema(description = "标签")
    private List<@Size(max = 32, message = "单个标签最多32个字符") String> tags;

    @NotNull(message = "可见范围不能为空")
    @Schema(description = "可见范围")
    private LinkVisibilityScope visibilityScope;

    @Valid
    @Size(max = 200, message = "可见目标最多200个")
    @Schema(description = "指定部门或指定用户目标")
    private List<LinkVisibilityTargetCommand> visibilityTargets;

    @NotNull(groups = LinkStrictValidation.class, message = "严格校验时推荐标记不能为空")
    @Schema(description = "是否推荐")
    private Boolean recommended;

    @Min(value = 0, message = "排序号不能小于0")
    @Max(value = 999999, message = "排序号不能大于999999")
    @Schema(description = "排序号")
    private Integer sortNo;

    @Size(max = 256, message = "备注最多256个字符")
    @Schema(description = "后台备注")
    private String remark;
}
