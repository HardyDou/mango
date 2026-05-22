package io.mango.calendar.core.service;

import io.mango.calendar.api.query.AddWorkdaysQuery;
import io.mango.calendar.api.query.BatchCheckWorkdayQuery;
import io.mango.calendar.api.query.CalendarDateQuery;
import io.mango.calendar.api.query.CountWorkdaysQuery;
import io.mango.calendar.api.query.DateRangeQuery;
import io.mango.calendar.api.query.LunarDateQuery;
import io.mango.calendar.api.query.MonthQuery;
import io.mango.calendar.api.query.NthWorkdayOfMonthQuery;
import io.mango.calendar.api.query.SolarDateQuery;
import io.mango.calendar.api.query.SolarTermYearQuery;
import io.mango.calendar.api.vo.CalendarDayVO;
import io.mango.calendar.api.vo.LunarDayInfoVO;
import io.mango.calendar.api.vo.MonthWorkdaySummaryVO;
import io.mango.calendar.api.vo.SolarTermVO;

import java.time.LocalDate;
import java.util.List;

public interface ICalendarService {

    CalendarDayVO getDay(CalendarDateQuery query);

    boolean isWorkday(CalendarDateQuery query);

    LocalDate nextWorkday(CalendarDateQuery query);

    LocalDate previousWorkday(CalendarDateQuery query);

    LocalDate addWorkdays(AddWorkdaysQuery query);

    int countWorkdays(CountWorkdaysQuery query);

    List<CalendarDayVO> listDays(DateRangeQuery query);

    List<CalendarDayVO> batchCheck(BatchCheckWorkdayQuery query);

    MonthWorkdaySummaryVO monthSummary(MonthQuery query);

    LocalDate firstWorkdayOfMonth(MonthQuery query);

    LocalDate lastWorkdayOfMonth(MonthQuery query);

    LocalDate nthWorkdayOfMonth(NthWorkdayOfMonthQuery query);

    LunarDayInfoVO lunarDay(SolarDateQuery query);

    LocalDate lunarToSolar(LunarDateQuery query);

    List<SolarTermVO> solarTerms(SolarTermYearQuery query);
}
