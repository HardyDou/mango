package io.mango.cms.api.command;

import io.mango.cms.api.validation.CmsStrictValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SaveCmsBannerCommand {

    @Schema(description = "主键 ID")
    @Positive(message = "主键 ID 必须大于 0")
    private Long id;

    @NotNull(message = "站点 ID 不能为空")
    @Schema(description = "站点 ID")
    private Long siteId;

    @NotBlank(message = "展示位置不能为空")
    @Size(max = 64, message = "展示位置最多64个字符")
    @Schema(description = "展示位置")
    private String position;

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题最多255个字符")
    @Schema(description = "标题")
    private String title;

    @Size(max = 255, message = "副标题最多255个字符")
    @Schema(description = "副标题")
    private String subtitle;

    @NotBlank(message = "媒体类型不能为空")
    @Pattern(regexp = "IMAGE|VIDEO", message = "媒体类型不合法")
    @Schema(description = "媒体类型")
    private String mediaType;

    @Size(max = 128, message = "媒体文件 ID 最多128个字符")
    @Schema(description = "媒体文件 ID")
    private String mediaFileId;

    @Size(max = 512, message = "跳转地址最多512个字符")
    @Schema(description = "跳转地址")
    private String jumpUrl;

    @Schema(description = "开始时间")
    @NotNull(groups = CmsStrictValidation.class, message = "严格展示模式下开始时间不能为空")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @NotNull(groups = CmsStrictValidation.class, message = "严格展示模式下结束时间不能为空")
    private LocalDateTime endTime;

    @Schema(description = "排序值")
    @PositiveOrZero(message = "排序值不能小于 0")
    private Integer sort;

    @Pattern(regexp = "ENABLED|DISABLED", message = "状态不合法")
    @Schema(description = "状态")
    private String status;
}
