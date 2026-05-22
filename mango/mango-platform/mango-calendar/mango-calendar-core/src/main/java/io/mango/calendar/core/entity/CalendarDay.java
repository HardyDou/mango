package io.mango.calendar.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("calendar_day")
public class CalendarDay {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long calendarId;

    private Integer calendarYear;

    private LocalDate calendarDate;

    private Integer dayOfWeek;

    private String dayType;

    private Integer workday;

    private String dayName;

    private Integer lunarYear;

    private Integer lunarMonth;

    private Integer lunarDay;

    private Integer lunarLeapMonth;

    private String lunarText;

    private String ganzhiYear;

    private String zodiac;

    private String solarTerm;

    private String source;

    private String remark;

    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
