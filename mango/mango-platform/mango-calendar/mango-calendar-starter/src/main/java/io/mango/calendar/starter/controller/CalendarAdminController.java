package io.mango.calendar.starter.controller;

import io.mango.calendar.api.CalendarAdminApi;
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
import io.mango.calendar.core.service.ICalendarAdminService;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/calendar/admin")
@RequiredArgsConstructor
@Validated
@Tag(name = "日历管理", description = "维护日历定义、年度日期和工作日属性")
public class CalendarAdminController implements CalendarAdminApi {

    private final ICalendarAdminService calendarAdminService;

    @Override
    @GetMapping("/calendars/page")
    @Operation(summary = "分页查询日历", description = "分页查询当前租户下的日历定义")
    public R<PageResult<CalendarVO>> pageCalendars(@Valid @ParameterObject CalendarPageQuery query) {
        return R.ok(calendarAdminService.pageCalendars(query));
    }

    @Override
    @GetMapping("/calendars/options")
    @Operation(summary = "查询日历选项", description = "查询当前租户下可选日历")
    public R<List<CalendarOptionVO>> listCalendarOptions(@Valid @ParameterObject CalendarOptionQuery query) {
        return R.ok(calendarAdminService.listCalendarOptions(query));
    }

    @Override
    @PostMapping("/calendars")
    @Operation(summary = "新增日历", description = "新增一个工作日历定义")
    public R<Long> createCalendar(@Valid @RequestBody CreateCalendarCommand command) {
        return R.ok(calendarAdminService.createCalendar(command));
    }

    @Override
    @PutMapping("/calendars")
    @Operation(summary = "更新日历", description = "更新日历名称等基础信息")
    public R<Boolean> updateCalendar(@Valid @RequestBody UpdateCalendarCommand command) {
        return R.ok(calendarAdminService.updateCalendar(command));
    }

    @Override
    @PutMapping("/calendars/status")
    @Operation(summary = "更新日历状态", description = "启用或停用日历")
    public R<Boolean> updateCalendarStatus(@Valid @RequestBody UpdateCalendarStatusCommand command) {
        return R.ok(calendarAdminService.updateCalendarStatus(command));
    }

    @Override
    @DeleteMapping("/calendars")
    @Operation(summary = "删除日历", description = "删除日历及其全部年度日期")
    public R<Boolean> deleteCalendar(
            @Parameter(description = "日历 ID", required = true)
            @NotNull(message = "日历 ID 不能为空")
            @RequestParam Long id) {
        return R.ok(calendarAdminService.deleteCalendar(id));
    }

    @Override
    @GetMapping("/years/page")
    @Operation(summary = "分页查询年度", description = "分页查询已初始化的日历年度")
    public R<PageResult<CalendarYearSummaryVO>> pageCalendarYears(@Valid @ParameterObject CalendarYearPageQuery query) {
        return R.ok(calendarAdminService.pageCalendarYears(query));
    }

    @Override
    @PostMapping("/years/init")
    @Operation(summary = "初始化年度", description = "生成指定年度 365/366 天的完整日历数据")
    public R<Boolean> initCalendarYear(@Valid @RequestBody InitCalendarYearCommand command) {
        return R.ok(calendarAdminService.initCalendarYear(command));
    }

    @Override
    @PutMapping("/years/lunar")
    @Operation(summary = "刷新年度农历信息", description = "为指定年度全部日期重新计算农历、生肖、干支纪年和节气")
    public R<Boolean> refreshCalendarYearLunar(@Valid @RequestBody RefreshCalendarYearLunarCommand command) {
        return R.ok(calendarAdminService.refreshCalendarYearLunar(command));
    }

    @Override
    @PutMapping("/years/enabled")
    @Operation(summary = "更新年度启用状态", description = "启用或停用指定年度的全部日期")
    public R<Boolean> updateCalendarYearEnabled(@Valid @RequestBody UpdateCalendarYearEnabledCommand command) {
        return R.ok(calendarAdminService.updateCalendarYearEnabled(command));
    }

    @Override
    @DeleteMapping("/years")
    @Operation(summary = "删除年度", description = "删除指定日历年度下的全部日期")
    public R<Boolean> deleteCalendarYear(
            @Parameter(description = "日历编码", required = true)
            @NotBlank(message = "日历编码不能为空")
            @RequestParam String calendarCode,
            @Parameter(description = "年度", required = true)
            @NotNull(message = "年度不能为空")
            @Min(value = 1900, message = "年度不能早于1900")
            @Max(value = 2100, message = "年度不能晚于2100")
            @RequestParam Integer year) {
        return R.ok(calendarAdminService.deleteCalendarYear(calendarCode, year));
    }

    @Override
    @GetMapping("/years/summary")
    @Operation(summary = "查询年度汇总", description = "查询指定年度的工作日和假日统计")
    public R<CalendarYearSummaryVO> yearSummary(@Valid @ParameterObject CalendarYearSummaryQuery query) {
        return R.ok(calendarAdminService.yearSummary(query));
    }

    @Override
    @GetMapping("/days/page")
    @Operation(summary = "分页查询日期", description = "分页查询日历年度日期明细")
    public R<PageResult<CalendarDayVO>> pageCalendarDays(@Valid @ParameterObject CalendarDayPageQuery query) {
        return R.ok(calendarAdminService.pageCalendarDays(query));
    }

    @Override
    @PutMapping("/days")
    @Operation(summary = "更新日期", description = "更新单个日期的工作日属性")
    public R<Boolean> updateCalendarDay(@Valid @RequestBody UpdateCalendarDayCommand command) {
        return R.ok(calendarAdminService.updateCalendarDay(command));
    }

    @Override
    @DeleteMapping("/days")
    @Operation(summary = "删除日期", description = "删除指定日历日期明细")
    public R<Boolean> deleteCalendarDay(
            @Parameter(description = "日期 ID", required = true)
            @NotNull(message = "日期 ID 不能为空")
            @RequestParam Long id) {
        return R.ok(calendarAdminService.deleteCalendarDay(id));
    }

    @Override
    @PutMapping("/days/batch")
    @Operation(summary = "批量更新日期", description = "批量更新多个日期的工作日属性")
    public R<Boolean> batchUpdateCalendarDays(@Valid @RequestBody BatchUpdateCalendarDaysCommand command) {
        return R.ok(calendarAdminService.batchUpdateCalendarDays(command));
    }

    @Override
    @PostMapping("/days/import")
    @Operation(summary = "导入日期", description = "按日期覆盖导入工作日属性")
    public R<Boolean> importCalendarDays(@Valid @RequestBody ImportCalendarDaysCommand command) {
        return R.ok(calendarAdminService.importCalendarDays(command));
    }
}
