package io.mango.calendar.core.service;

import io.mango.calendar.api.command.BatchUpdateCalendarDaysCommand;
import io.mango.calendar.api.command.CreateCalendarCommand;
import io.mango.calendar.api.command.ImportCalendarDaysCommand;
import io.mango.calendar.api.command.InitCalendarYearCommand;
import io.mango.calendar.api.command.RefreshCalendarYearLunarCommand;
import io.mango.calendar.api.command.UpdateCalendarCommand;
import io.mango.calendar.api.command.UpdateCalendarDayCommand;
import io.mango.calendar.api.command.UpdateCalendarStatusCommand;
import io.mango.calendar.api.command.UpdateCalendarYearEnabledCommand;
import io.mango.calendar.api.query.CalendarDayPageQuery;
import io.mango.calendar.api.query.CalendarOptionQuery;
import io.mango.calendar.api.query.CalendarPageQuery;
import io.mango.calendar.api.query.CalendarYearPageQuery;
import io.mango.calendar.api.query.CalendarYearSummaryQuery;
import io.mango.calendar.api.vo.CalendarDayVO;
import io.mango.calendar.api.vo.CalendarOptionVO;
import io.mango.calendar.api.vo.CalendarVO;
import io.mango.calendar.api.vo.CalendarYearSummaryVO;
import io.mango.common.vo.PageResult;

import java.util.List;

public interface ICalendarAdminService {

    PageResult<CalendarVO> pageCalendars(CalendarPageQuery query);

    List<CalendarOptionVO> listCalendarOptions(CalendarOptionQuery query);

    Long createCalendar(CreateCalendarCommand command);

    boolean updateCalendar(UpdateCalendarCommand command);

    boolean updateCalendarStatus(UpdateCalendarStatusCommand command);

    boolean deleteCalendar(Long id);

    PageResult<CalendarYearSummaryVO> pageCalendarYears(CalendarYearPageQuery query);

    boolean initCalendarYear(InitCalendarYearCommand command);

    boolean refreshCalendarYearLunar(RefreshCalendarYearLunarCommand command);

    boolean updateCalendarYearEnabled(UpdateCalendarYearEnabledCommand command);

    boolean deleteCalendarYear(String calendarCode, Integer year);

    CalendarYearSummaryVO yearSummary(CalendarYearSummaryQuery query);

    PageResult<CalendarDayVO> pageCalendarDays(CalendarDayPageQuery query);

    boolean updateCalendarDay(UpdateCalendarDayCommand command);

    boolean deleteCalendarDay(Long id);

    boolean batchUpdateCalendarDays(BatchUpdateCalendarDaysCommand command);

    boolean importCalendarDays(ImportCalendarDaysCommand command);
}
