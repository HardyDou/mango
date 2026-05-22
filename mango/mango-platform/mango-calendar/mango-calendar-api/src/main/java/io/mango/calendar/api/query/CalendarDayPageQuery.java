package io.mango.calendar.api.query;

import io.mango.calendar.api.enums.CalendarDayType;
import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "日历日期分页查询")
public class CalendarDayPageQuery extends PageQuery {

    @Schema(description = "日历编码")
    private String calendarCode;

    @Schema(description = "年度，例如 2026")
    private Integer year;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "日期类型")
    private CalendarDayType dayType;

    @Schema(description = "是否工作日")
    private Boolean workday;

    @Schema(description = "启用状态：1-启用，0-停用")
    private Integer enabled;

    @Schema(description = "关键词。支持日期名称、来源、备注模糊搜索")
    private String keyword;
}
