package io.mango.area.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "行政区划视图")
public class SysAreaVO {
    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "父级 ID")
    private Long pid;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "地区首字母")
    private String letter;
    @Schema(description = "行政区划编码")
    private Long adcode;
    @Schema(description = "位置")
    private String location;
    @Schema(description = "行政区划排序")
    private Integer areaSort;
    @Schema(description = "行政区划状态")
    private String areaStatus;
    @Schema(description = "行政区划类型")
    private String areaType;
    @Schema(description = "是否热门")
    private String hot;
    @Schema(description = "城市编码")
    private String cityCode;
    @Schema(description = "租户 ID")
    private Long tenantId;
}
