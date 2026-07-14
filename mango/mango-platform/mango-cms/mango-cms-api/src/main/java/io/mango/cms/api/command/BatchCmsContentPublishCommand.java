package io.mango.cms.api.command;

import io.mango.cms.api.validation.CmsStrictValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BatchCmsContentPublishCommand {

    @NotEmpty(message = "内容 ID 不能为空")
    @Size(max = 200, message = "内容 ID 最多200个")
    @Schema(description = "内容 ID 列表")
    private List<Long> contentIds;

    @NotNull(message = "站点 ID 不能为空")
    @Schema(description = "站点 ID")
    private Long siteId;

    @NotEmpty(message = "栏目 ID 不能为空")
    @Size(max = 200, message = "栏目 ID 最多200个")
    @Schema(description = "分类 ID 列表")
    private List<Long> categoryIds;

    @Schema(description = "发布时间")
    @NotNull(groups = CmsStrictValidation.class, message = "严格发布模式下发布时间不能为空")
    private LocalDateTime publishTime;

    @Schema(description = "计划发布时间")
    @NotNull(groups = CmsStrictValidation.class, message = "严格发布模式下计划发布时间不能为空")
    private LocalDateTime scheduledPublishTime;

    @Schema(description = "下线时间")
    @NotNull(groups = CmsStrictValidation.class, message = "严格发布模式下下线时间不能为空")
    private LocalDateTime offlineTime;

    @Schema(description = "是否置顶")
    @NotNull(groups = CmsStrictValidation.class, message = "严格发布模式下置顶标记不能为空")
    private Boolean top;

    @Pattern(regexp = "NONE|CATEGORY|SITE", message = "置顶范围不合法")
    @Schema(description = "置顶范围")
    private String topScope;

    @Schema(description = "是否推荐")
    @NotNull(groups = CmsStrictValidation.class, message = "严格发布模式下推荐标记不能为空")
    private Boolean recommended;

    @Pattern(regexp = "NONE|HOME|HOT|EDITOR", message = "推荐类型不合法")
    @Schema(description = "推荐类型")
    private String recommendationType;

    @Schema(description = "排序值")
    @PositiveOrZero(message = "排序值不能小于 0")
    private Integer sort;
}
