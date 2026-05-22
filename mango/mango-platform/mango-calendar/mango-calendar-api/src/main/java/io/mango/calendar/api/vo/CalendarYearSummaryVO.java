package io.mango.calendar.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "日历年度汇总")
public class CalendarYearSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日历编码")
    private String calendarCode;

    @Schema(description = "日历名称")
    private String calendarName;

    @Schema(description = "年度")
    private Integer year;

    @Schema(description = "总天数")
    private Integer totalDays;

    @Schema(description = "工作日数量")
    private Integer workdays;

    @Schema(description = "休息日数量")
    private Integer restdays;

    @Schema(description = "法定假日数量")
    private Integer legalHolidays;

    @Schema(description = "调休工作日数量")
    private Integer adjustedWorkdays;

    @Schema(description = "临时休息日数量")
    private Integer tempClosedDays;

    @Schema(description = "临时工作日数量")
    private Integer tempOpenDays;

    @Schema(description = "启用状态：1-启用，0-停用")
    private Integer enabled;
}
