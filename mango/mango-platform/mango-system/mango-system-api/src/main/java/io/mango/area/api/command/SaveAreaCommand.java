package io.mango.area.api.command;

import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "保存行政区划命令")
public class SaveAreaCommand {

    @Schema(description = "行政区划 ID；修改时必填")
    @Positive(message = "主键 ID必须大于 0")
    private Long id;

    @NotNull(message = "父级行政区划 ID 不能为空")
    @Schema(description = "父级行政区划 ID，根节点为 0")
    private Long pid;

    @NotBlank(message = "地区名称不能为空")
    @Size(max = 100, message = "地区名称长度不能超过 100")
    @Schema(description = "名称")
    private String name;

    @Size(max = 32, message = "地区首字母长度不能超过 32")
    @Schema(description = "地区首字母")
    private String letter;

    @Schema(description = "行政区划编码")
    @Positive(message = "行政区划编码必须大于 0")
    private Long adcode;

    @Size(max = 100, message = "经纬度长度不能超过 100")
    @Schema(description = "位置")
    private String location;

    @Min(value = 0, message = "排序值不能小于 0")
    @Schema(description = "行政区划排序")
    private Integer areaSort;

    @NotBlank(message = "行政区划状态不能为空")
    @Schema(description = "行政区划状态")
    private String areaStatus;

    @NotBlank(message = "行政区划类型不能为空")
    @Schema(description = "行政区划类型")
    private String areaType;

    @Schema(description = "是否热门")
    @Size(max = 8, message = "是否热门长度不正确")
    private String hot;

    @Size(max = 32, message = "城市编码长度不能超过 32")
    @Schema(description = "城市编码")
    private String cityCode;

    @Min(value = 1, message = "租户 ID 必须大于 0")
    @Max(value = Long.MAX_VALUE, message = "租户 ID 不正确")
    @Schema(description = "租户 ID")
    private Long tenantId;
}
