package io.mango.calendar.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Schema(description = "月份工作日汇总")
public class MonthWorkdaySummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日历编码")
    private String calendarCode;

    @Schema(description = "年度")
    private Integer year;

    @Schema(description = "月份")
    private Integer month;

    @Schema(description = "月份总天数")
    private Integer totalDays;

    @Schema(description = "工作日数量")
    private Integer workdays;

    @Schema(description = "休息日数量")
    private Integer restdays;

    @Schema(description = "第一个工作日")
    private LocalDate firstWorkday;

    @Schema(description = "最后一个工作日")
    private LocalDate lastWorkday;
}
