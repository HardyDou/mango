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
public class SaveCmsAdDeliveryCommand {

    @Schema(description = "主键 ID")
    @Positive(message = "主键 ID 必须大于 0")
    private Long id;

    @NotNull(message = "站点 ID 不能为空")
    @Schema(description = "站点 ID")
    private Long siteId;

    @NotNull(message = "广告位 ID 不能为空")
    @Schema(description = "广告位 ID")
    private Long adId;

    @NotBlank(message = "投放名称不能为空")
    @Size(max = 128, message = "投放名称最多128个字符")
    @Schema(description = "投放名称")
    private String deliveryName;

    @NotBlank(message = "物料类型不能为空")
    @Pattern(regexp = "TEXT|RICH_TEXT|HTML|IMAGE|SINGLE_IMAGE|MULTI_IMAGE|VIDEO", message = "物料类型不合法")
    @Schema(description = "素材类型")
    private String materialType;

    @Size(max = 255, message = "标题最多255个字符")
    @Schema(description = "标题")
    private String title;

    @Size(max = 1024, message = "文本内容最多1024个字符")
    @Schema(description = "文本内容")
    private String textContent;

    @Schema(description = "富文本内容")
    @Size(max = 65535, message = "富文本内容最多65535个字符")
    private String richContent;

    @Schema(description = "HTML 内容")
    @Size(max = 65535, message = "HTML 内容最多65535个字符")
    private String htmlContent;

    @Size(max = 128, message = "图片文件 ID 最多128个字符")
    @Schema(description = "图片文件 ID")
    private String imageFileId;

    @Size(max = 1024, message = "多图片文件 ID 最多1024个字符")
    @Schema(description = "图片文件 ID 列表")
    private String imageFileIds;

    @Size(max = 128, message = "视频文件 ID 最多128个字符")
    @Schema(description = "视频文件 ID")
    private String videoFileId;

    @Size(max = 128, message = "封面文件 ID 最多128个字符")
    @Schema(description = "封面文件 ID")
    private String coverFileId;

    @Size(max = 512, message = "跳转地址最多512个字符")
    @Schema(description = "跳转地址")
    private String jumpUrl;

    @Pattern(regexp = "SELF|BLANK", message = "打开方式不合法")
    @Schema(description = "打开目标")
    private String openTarget;

    @Schema(description = "开始时间")
    @NotNull(groups = CmsStrictValidation.class, message = "严格投放模式下开始时间不能为空")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @NotNull(groups = CmsStrictValidation.class, message = "严格投放模式下结束时间不能为空")
    private LocalDateTime endTime;

    @Schema(description = "排序值")
    @PositiveOrZero(message = "排序值不能小于 0")
    private Integer sort;

    @Pattern(regexp = "ENABLED|DISABLED", message = "状态不合法")
    @Schema(description = "状态")
    private String status;
}
