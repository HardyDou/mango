package io.mango.calendar.api;

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
import io.mango.common.result.R;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

@Validated
public interface CalendarApi {

    R<CalendarDayVO> getDay(@Valid CalendarDateQuery query);

    R<Boolean> isWorkday(@Valid CalendarDateQuery query);

    R<LocalDate> nextWorkday(@Valid CalendarDateQuery query);

    R<LocalDate> previousWorkday(@Valid CalendarDateQuery query);

    R<LocalDate> addWorkdays(@Valid AddWorkdaysQuery query);

    R<Integer> countWorkdays(@Valid CountWorkdaysQuery query);

    R<List<CalendarDayVO>> listDays(@Valid DateRangeQuery query);

    R<List<CalendarDayVO>> batchCheck(@Valid BatchCheckWorkdayQuery query);

    R<MonthWorkdaySummaryVO> monthSummary(@Valid MonthQuery query);

    R<LocalDate> firstWorkdayOfMonth(@Valid MonthQuery query);

    R<LocalDate> lastWorkdayOfMonth(@Valid MonthQuery query);

    R<LocalDate> nthWorkdayOfMonth(@Valid NthWorkdayOfMonthQuery query);

    R<LunarDayInfoVO> lunarDay(@Valid SolarDateQuery query);

    R<LocalDate> lunarToSolar(@Valid LunarDateQuery query);

    R<List<SolarTermVO>> solarTerms(@Valid SolarTermYearQuery query);
}
