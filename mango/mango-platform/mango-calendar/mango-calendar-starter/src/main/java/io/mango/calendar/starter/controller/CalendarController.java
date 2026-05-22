package io.mango.calendar.starter.controller;

import io.mango.calendar.api.CalendarApi;
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
import io.mango.calendar.core.service.ICalendarService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
@Tag(name = "工作日历计算", description = "按已维护年度日历提供工作日判断、日期偏移、区间统计和月份工作日查询")
public class CalendarController implements CalendarApi {

    private final ICalendarService calendarService;

    @Override
    @GetMapping("/workdays/day")
    @Operation(summary = "查询日期", description = "查询指定日期的最终日历属性")
    public R<CalendarDayVO> getDay(@Valid @ParameterObject CalendarDateQuery query) {
        return R.ok(calendarService.getDay(query));
    }

    @Override
    @GetMapping("/workdays/check")
    @Operation(summary = "判断工作日", description = "判断指定日历中的指定日期是否为工作日")
    public R<Boolean> isWorkday(@Valid @ParameterObject CalendarDateQuery query) {
        return R.ok(calendarService.isWorkday(query));
    }

    @Override
    @GetMapping("/workdays/next")
    @Operation(summary = "查询下一个工作日", description = "从指定日期次日开始查询下一个工作日")
    public R<LocalDate> nextWorkday(@Valid @ParameterObject CalendarDateQuery query) {
        return R.ok(calendarService.nextWorkday(query));
    }

    @Override
    @GetMapping("/workdays/previous")
    @Operation(summary = "查询上一个工作日", description = "从指定日期前一日开始查询上一个工作日")
    public R<LocalDate> previousWorkday(@Valid @ParameterObject CalendarDateQuery query) {
        return R.ok(calendarService.previousWorkday(query));
    }

    @Override
    @GetMapping("/workdays/add")
    @Operation(summary = "工作日偏移", description = "按工作日数量计算偏移后的日期")
    public R<LocalDate> addWorkdays(@Valid @ParameterObject AddWorkdaysQuery query) {
        return R.ok(calendarService.addWorkdays(query));
    }

    @Override
    @GetMapping("/workdays/count")
    @Operation(summary = "计算区间工作日", description = "计算两个日期之间的工作日数量")
    public R<Integer> countWorkdays(@Valid @ParameterObject CountWorkdaysQuery query) {
        return R.ok(calendarService.countWorkdays(query));
    }

    @Override
    @GetMapping("/workdays/list")
    @Operation(summary = "查询区间日期", description = "查询区间内每天的最终日历属性")
    public R<List<CalendarDayVO>> listDays(@Valid @ParameterObject DateRangeQuery query) {
        return R.ok(calendarService.listDays(query));
    }

    @Override
    @PostMapping("/workdays/batch-check")
    @Operation(summary = "批量校验日期", description = "批量返回日期类型和是否工作日")
    public R<List<CalendarDayVO>> batchCheck(@Valid @RequestBody BatchCheckWorkdayQuery query) {
        return R.ok(calendarService.batchCheck(query));
    }

    @Override
    @GetMapping("/workdays/month/summary")
    @Operation(summary = "查询月份汇总", description = "查询月份工作日、休息日、第一个工作日和最后一个工作日")
    public R<MonthWorkdaySummaryVO> monthSummary(@Valid @ParameterObject MonthQuery query) {
        return R.ok(calendarService.monthSummary(query));
    }

    @Override
    @GetMapping("/workdays/month/first")
    @Operation(summary = "查询月份第一个工作日", description = "查询指定月份的第一个工作日")
    public R<LocalDate> firstWorkdayOfMonth(@Valid @ParameterObject MonthQuery query) {
        return R.ok(calendarService.firstWorkdayOfMonth(query));
    }

    @Override
    @GetMapping("/workdays/month/last")
    @Operation(summary = "查询月份最后一个工作日", description = "查询指定月份的最后一个工作日")
    public R<LocalDate> lastWorkdayOfMonth(@Valid @ParameterObject MonthQuery query) {
        return R.ok(calendarService.lastWorkdayOfMonth(query));
    }

    @Override
    @GetMapping("/workdays/month/nth")
    @Operation(summary = "查询月份第 N 个工作日", description = "查询指定月份的第 N 个工作日")
    public R<LocalDate> nthWorkdayOfMonth(@Valid @ParameterObject NthWorkdayOfMonthQuery query) {
        return R.ok(calendarService.nthWorkdayOfMonth(query));
    }

    @Override
    @GetMapping("/lunar/day")
    @Operation(summary = "查询农历", description = "按公历日期查询农历、生肖、干支纪年和当天节气")
    public R<LunarDayInfoVO> lunarDay(@Valid @ParameterObject SolarDateQuery query) {
        return R.ok(calendarService.lunarDay(query));
    }

    @Override
    @GetMapping("/lunar/to-solar")
    @Operation(summary = "农历转公历", description = "按农历年月日和是否闰月换算公历日期")
    public R<LocalDate> lunarToSolar(@Valid @ParameterObject LunarDateQuery query) {
        return R.ok(calendarService.lunarToSolar(query));
    }

    @Override
    @GetMapping("/lunar/solar-terms")
    @Operation(summary = "查询年度节气", description = "查询指定公历年度的二十四节气日期")
    public R<List<SolarTermVO>> solarTerms(@Valid @ParameterObject SolarTermYearQuery query) {
        return R.ok(calendarService.solarTerms(query));
    }
}
