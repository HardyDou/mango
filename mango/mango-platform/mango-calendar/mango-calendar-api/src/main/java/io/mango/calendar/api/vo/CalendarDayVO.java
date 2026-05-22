package io.mango.calendar.api.vo;

import io.mango.calendar.api.enums.CalendarDayType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "日历日期")
public class CalendarDayVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日期 ID")
    private Long id;

    @Schema(description = "日历编码")
    private String calendarCode;

    @Schema(description = "日历名称")
    private String calendarName;

    @Schema(description = "年度")
    private Integer calendarYear;

    @Schema(description = "日期")
    private LocalDate date;

    @Schema(description = "星期：1-周一，7-周日")
    private Integer dayOfWeek;

    @Schema(description = "日期类型")
    private CalendarDayType dayType;

    @Schema(description = "是否工作日")
    private boolean workday;

    @Schema(description = "日期名称")
    private String dayName;

    @Schema(description = "农历年")
    private Integer lunarYear;

    @Schema(description = "农历月")
    private Integer lunarMonth;

    @Schema(description = "农历日")
    private Integer lunarDay;

    @Schema(description = "是否农历闰月")
    private boolean lunarLeapMonth;

    @Schema(description = "农历中文日期")
    private String lunarText;

    @Schema(description = "干支纪年")
    private String ganzhiYear;

    @Schema(description = "生肖")
    private String zodiac;

    @Schema(description = "节气")
    private String solarTerm;

    @Schema(description = "数据来源")
    private String source;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "启用状态：1-启用，0-停用")
    private Integer enabled;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
