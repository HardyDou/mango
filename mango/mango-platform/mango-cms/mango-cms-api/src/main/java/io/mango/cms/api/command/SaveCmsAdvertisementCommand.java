package io.mango.cms.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveCmsAdvertisementCommand {

    @Schema(description = "主键 ID")
    @Positive(message = "主键 ID 必须大于 0")
    private Long id;

    @NotNull(message = "站点 ID 不能为空")
    @Schema(description = "站点 ID")
    private Long siteId;

    @NotBlank(message = "广告位编码不能为空")
    @Size(max = 64, message = "广告位编码最多64个字符")
    @Pattern(regexp = "[A-Za-z0-9_.:-]+", message = "广告位编码只能包含字母、数字、点、下划线、冒号和短横线")
    @Schema(description = "广告编码")
    private String adCode;

    @NotBlank(message = "广告位名称不能为空")
    @Size(max = 128, message = "广告位名称最多128个字符")
    @Schema(description = "广告名称")
    private String adName;

    @NotBlank(message = "广告位位置不能为空")
    @Size(max = 64, message = "广告位置最多64个字符")
    @Schema(description = "展示位置")
    private String position;

    @NotBlank(message = "位置类型不能为空")
    @Pattern(regexp = "BANNER|RECOMMEND|SIDEBAR|FOOTER|POPUP|CUSTOM", message = "位置类型不合法")
    @Schema(description = "位置类型")
    private String positionType;

    @Size(max = 255, message = "支持物料类型最多255个字符")
    @Schema(description = "支持的素材类型列表")
    private String supportedMaterialTypes;

    @Schema(description = "宽度")
    @PositiveOrZero(message = "宽度不能小于 0")
    private Integer width;

    @Schema(description = "高度")
    @PositiveOrZero(message = "高度不能小于 0")
    private Integer height;

    @Size(max = 512, message = "备注最多512个字符")
    @Schema(description = "备注")
    private String remark;

    @Schema(description = "排序值")
    @PositiveOrZero(message = "排序值不能小于 0")
    private Integer sort;

    @Pattern(regexp = "ENABLED|DISABLED", message = "状态不合法")
    @Schema(description = "状态")
    private String status;
}
