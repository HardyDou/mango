package io.mango.calendar.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "日历年度分页查询")
public class CalendarYearPageQuery extends PageQuery {

    @Schema(description = "日历编码")
    private String calendarCode;

    @Schema(description = "年度，例如 2026")
    private Integer year;

    @Schema(description = "启用状态：1-启用，0-停用")
    private Integer enabled;
}
