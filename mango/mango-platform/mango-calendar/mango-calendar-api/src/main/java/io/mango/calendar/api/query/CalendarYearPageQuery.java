package io.mango.calendar.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "日历年度分页查询")
public class CalendarYearPageQuery extends PageQuery {

    @Schema(description = "日历编码")
    @Size(max = 64, message = "日历编码不能超过64个字符")
    private String calendarCode;

    @Schema(description = "年度，例如 2026")
    @Min(value = 1900, message = "年度不能早于1900")
    @Max(value = 2100, message = "年度不能晚于2100")
    private Integer year;

    @Schema(description = "启用状态：1-启用，0-停用")
    @Min(value = 0, message = "启用状态只能是0或1")
    @Max(value = 1, message = "启用状态只能是0或1")
    private Integer enabled;
}
