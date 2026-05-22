package io.mango.calendar.api;

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
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 日历后台管理 API 契约。
 */
@Validated
public interface CalendarAdminApi {

    R<PageResult<CalendarVO>> pageCalendars(@Valid CalendarPageQuery query);

    R<List<CalendarOptionVO>> listCalendarOptions(@Valid CalendarOptionQuery query);

    R<Long> createCalendar(@Valid CreateCalendarCommand command);

    R<Boolean> updateCalendar(@Valid UpdateCalendarCommand command);

    R<Boolean> updateCalendarStatus(@Valid UpdateCalendarStatusCommand command);

    R<Boolean> deleteCalendar(@NotNull(message = "日历 ID 不能为空") Long id);

    R<PageResult<CalendarYearSummaryVO>> pageCalendarYears(@Valid CalendarYearPageQuery query);

    R<Boolean> initCalendarYear(@Valid InitCalendarYearCommand command);

    R<Boolean> refreshCalendarYearLunar(@Valid RefreshCalendarYearLunarCommand command);

    R<Boolean> updateCalendarYearEnabled(@Valid UpdateCalendarYearEnabledCommand command);

    R<Boolean> deleteCalendarYear(
            @NotBlank(message = "日历编码不能为空") String calendarCode,
            @NotNull(message = "年度不能为空")
            @Min(value = 1900, message = "年度不能早于1900")
            @Max(value = 2100, message = "年度不能晚于2100")
            Integer year);

    R<CalendarYearSummaryVO> yearSummary(@Valid CalendarYearSummaryQuery query);

    R<PageResult<CalendarDayVO>> pageCalendarDays(@Valid CalendarDayPageQuery query);

    R<Boolean> updateCalendarDay(@Valid UpdateCalendarDayCommand command);

    R<Boolean> deleteCalendarDay(@NotNull(message = "日期 ID 不能为空") Long id);

    R<Boolean> batchUpdateCalendarDays(@Valid BatchUpdateCalendarDaysCommand command);

    R<Boolean> importCalendarDays(@Valid ImportCalendarDaysCommand command);
}
