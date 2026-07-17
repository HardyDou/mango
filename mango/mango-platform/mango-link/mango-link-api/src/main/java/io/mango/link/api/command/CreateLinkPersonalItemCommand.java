package io.mango.link.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

/**
 * 新增个人网址命令。
 */
@Data
@Schema(description = "新增个人网址命令")
public class CreateLinkPersonalItemCommand {

    @NotBlank(message = "网址名称不能为空")
    @Size(max = 128, message = "网址名称最多128个字符")
    @Schema(description = "网址名称")
    private String name;

    @NotBlank(message = "网址地址不能为空")
    @Size(max = 1024, message = "网址地址最多1024个字符")
    @Schema(description = "网址地址")
    private String url;

    @Positive(message = "分类 ID 必须大于0")
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

    @Size(max = 256, message = "备注最多256个字符")
    @Schema(description = "备注")
    private String remark;
}
