package io.mango.calendar.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("calendar_day")
public class CalendarDayEntity extends TenantEntity {

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

}
