package io.mango.calendar.core.service.impl;

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
import io.mango.calendar.core.config.CalendarKvProperties;
import io.mango.calendar.core.entity.Calendar;
import io.mango.calendar.core.entity.CalendarDay;
import io.mango.calendar.core.mapper.CalendarDayMapper;
import io.mango.calendar.core.mapper.CalendarMapper;
import io.mango.calendar.core.service.ICalendarLunarService;
import io.mango.calendar.core.service.ICalendarService;
import io.mango.calendar.core.support.CalendarDayTypes;
import io.mango.calendar.core.support.CalendarSupport;
import io.mango.common.result.Require;
import io.mango.infra.kv.api.ICache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(CalendarKvProperties.class)
public class CalendarServiceImpl implements ICalendarService {

    private final CalendarMapper calendarMapper;
    private final CalendarDayMapper dayMapper;
    private final ICalendarLunarService lunarService;
    private final ObjectProvider<ICache> cacheProvider;
    private final CalendarKvProperties kvProperties;

    @Override
    public CalendarDayVO getDay(CalendarDateQuery query) {
        Long tenantId = CalendarSupport.currentTenantId();
        String calendarCode = CalendarSupport.trimRequired(query.getCalendarCode(), "日历编码不能为空");
        Calendar calendar = ensureCalendarActive(tenantId, calendarCode);
        CalendarDay day = selectEnabledDayRequired(tenantId, calendar, query.getDate());
        return toDayVO(day, calendar);
    }

    @Override
    public boolean isWorkday(CalendarDateQuery query) {
        return getDay(query).isWorkday();
    }

    @Override
    public LocalDate nextWorkday(CalendarDateQuery query) {
        return moveWorkday(query.getCalendarCode(), query.getDate(), 1);
    }

    @Override
    public LocalDate previousWorkday(CalendarDateQuery query) {
        return moveWorkday(query.getCalendarCode(), query.getDate(), -1);
    }

    @Override
    public LocalDate addWorkdays(AddWorkdaysQuery query) {
        Long tenantId = CalendarSupport.currentTenantId();
        String calendarCode = CalendarSupport.trimRequired(query.getCalendarCode(), "日历编码不能为空");
        Calendar calendar = ensureCalendarActive(tenantId, calendarCode);
        int amount = query.getAmount();
        if (amount == 0) {
            selectEnabledDayRequired(tenantId, calendar, query.getSourceDate());
            return query.getSourceDate();
        }
        int step = amount > 0 ? 1 : -1;
        int remaining = Math.abs(amount);
        boolean includeSource = Boolean.TRUE.equals(query.getIncludeSource());
        LocalDate date = includeSource ? query.getSourceDate() : query.getSourceDate().plusDays(step);
        while (remaining > 0) {
            CalendarDay day = selectEnabledDayRequired(tenantId, calendar, date);
            if (day.getWorkday() == 1) {
                remaining--;
                if (remaining == 0) {
                    return date;
                }
            }
            date = date.plusDays(step);
        }
        return date;
    }

    @Override
    public int countWorkdays(CountWorkdaysQuery query) {
        Long tenantId = CalendarSupport.currentTenantId();
        String calendarCode = CalendarSupport.trimRequired(query.getCalendarCode(), "日历编码不能为空");
        Calendar calendar = ensureCalendarActive(tenantId, calendarCode);
        Require.isFalse(query.getStartDate().isAfter(query.getEndDate()), "开始日期不能晚于结束日期");
        LocalDate startDate = Boolean.FALSE.equals(query.getIncludeStart()) ? query.getStartDate().plusDays(1) : query.getStartDate();
        LocalDate endDate = Boolean.FALSE.equals(query.getIncludeEnd()) ? query.getEndDate().minusDays(1) : query.getEndDate();
        if (startDate.isAfter(endDate)) {
            return 0;
        }
        List<CalendarDay> days = selectRangeRequired(tenantId, calendar, startDate, endDate);
        return (int) days.stream().filter(day -> day.getEnabled() == 1 && day.getWorkday() == 1).count();
    }

    @Override
    public List<CalendarDayVO> listDays(DateRangeQuery query) {
        Long tenantId = CalendarSupport.currentTenantId();
        String calendarCode = CalendarSupport.trimRequired(query.getCalendarCode(), "日历编码不能为空");
        Calendar calendar = ensureCalendarActive(tenantId, calendarCode);
        Require.isFalse(query.getStartDate().isAfter(query.getEndDate()), "开始日期不能晚于结束日期");
        return selectRangeRequired(tenantId, calendar, query.getStartDate(), query.getEndDate()).stream()
                .map(day -> toDayVO(day, calendar))
                .toList();
    }

    @Override
    public List<CalendarDayVO> batchCheck(BatchCheckWorkdayQuery query) {
        Long tenantId = CalendarSupport.currentTenantId();
        String calendarCode = CalendarSupport.trimRequired(query.getCalendarCode(), "日历编码不能为空");
        Calendar calendar = ensureCalendarActive(tenantId, calendarCode);
        return query.getDates().stream()
                .map(date -> toDayVO(selectEnabledDayRequired(tenantId, calendar, date), calendar))
                .toList();
    }

    @Override
    public MonthWorkdaySummaryVO monthSummary(MonthQuery query) {
        List<CalendarDay> days = monthDays(query);
        MonthWorkdaySummaryVO vo = new MonthWorkdaySummaryVO();
        vo.setCalendarCode(query.getCalendarCode());
        vo.setYear(query.getYear());
        vo.setMonth(query.getMonth());
        vo.setTotalDays(days.size());
        vo.setWorkdays((int) days.stream().filter(day -> day.getEnabled() == 1 && day.getWorkday() == 1).count());
        vo.setRestdays(vo.getTotalDays() - vo.getWorkdays());
        vo.setFirstWorkday(days.stream().filter(day -> day.getEnabled() == 1 && day.getWorkday() == 1)
                .map(CalendarDay::getCalendarDate).findFirst().orElse(null));
        vo.setLastWorkday(days.stream().filter(day -> day.getEnabled() == 1 && day.getWorkday() == 1)
                .map(CalendarDay::getCalendarDate).reduce((first, second) -> second).orElse(null));
        return vo;
    }

    @Override
    public LocalDate firstWorkdayOfMonth(MonthQuery query) {
        return monthDays(query).stream()
                .filter(day -> day.getEnabled() == 1 && day.getWorkday() == 1)
                .map(CalendarDay::getCalendarDate)
                .findFirst()
                .orElse(null);
    }

    @Override
    public LocalDate lastWorkdayOfMonth(MonthQuery query) {
        return monthDays(query).stream()
                .filter(day -> day.getEnabled() == 1 && day.getWorkday() == 1)
                .map(CalendarDay::getCalendarDate)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    @Override
    public LocalDate nthWorkdayOfMonth(NthWorkdayOfMonthQuery query) {
        List<LocalDate> workdays = monthDays(query).stream()
                .filter(day -> day.getEnabled() == 1 && day.getWorkday() == 1)
                .map(CalendarDay::getCalendarDate)
                .toList();
        Require.isTrue(workdays.size() >= query.getNth(), "月份工作日数量不足");
        return workdays.get(query.getNth() - 1);
    }

    @Override
    public LunarDayInfoVO lunarDay(SolarDateQuery query) {
        return lunarService.getLunarDay(query);
    }

    @Override
    public LocalDate lunarToSolar(LunarDateQuery query) {
        return lunarService.lunarToSolar(query);
    }

    @Override
    public List<SolarTermVO> solarTerms(SolarTermYearQuery query) {
        return lunarService.listSolarTerms(query);
    }

    private LocalDate moveWorkday(String calendarCode, LocalDate sourceDate, int step) {
        Long tenantId = CalendarSupport.currentTenantId();
        String code = CalendarSupport.trimRequired(calendarCode, "日历编码不能为空");
        Calendar calendar = ensureCalendarActive(tenantId, code);
        LocalDate date = sourceDate.plusDays(step);
        while (true) {
            CalendarDay day = selectEnabledDayRequired(tenantId, calendar, date);
            if (day.getWorkday() == 1) {
                return date;
            }
            date = date.plusDays(step);
        }
    }

    private List<CalendarDay> monthDays(MonthQuery query) {
        Long tenantId = CalendarSupport.currentTenantId();
        String calendarCode = CalendarSupport.trimRequired(query.getCalendarCode(), "日历编码不能为空");
        Calendar calendar = ensureCalendarActive(tenantId, calendarCode);
        YearMonth month = YearMonth.of(query.getYear(), query.getMonth());
        return selectRangeRequired(tenantId, calendar, month.atDay(1), month.atEndOfMonth());
    }

    private Calendar ensureCalendarActive(Long tenantId, String calendarCode) {
        Calendar calendar = calendarMapper.selectActiveByCode(tenantId, calendarCode);
        Require.notNull(calendar, "日历不存在或未启用：" + calendarCode);
        return calendar;
    }

    private CalendarDay selectEnabledDayRequired(Long tenantId, Calendar calendar, LocalDate date) {
        String cacheKey = dayCacheKey(tenantId, calendar.getId(), date);
        CalendarDay cached = readDayCache(cacheKey);
        if (cached != null) {
            return cached;
        }
        CalendarDay day = dayMapper.selectByDate(tenantId, calendar.getId(), date);
        Require.notNull(day, "年度日历未初始化：" + calendar.getCalendarCode() + " " + date.getYear());
        Require.isTrue(day.getEnabled() == 1, "年度日历未启用：" + calendar.getCalendarCode() + " " + date.getYear());
        writeDayCache(cacheKey, day);
        return day;
    }

    private List<CalendarDay> selectRangeRequired(Long tenantId, Calendar calendar, LocalDate startDate, LocalDate endDate) {
        List<CalendarDay> days = dayMapper.selectBetween(tenantId, calendar.getId(), startDate, endDate);
        long expected = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        Require.isTrue(days.size() == expected, "年度日历未初始化：" + calendar.getCalendarCode() + " " + startDate.getYear());
        return days;
    }

    private CalendarDay readDayCache(String cacheKey) {
        ICache cache = cacheProvider.getIfAvailable();
        if (cache == null) {
            return null;
        }
        String value = cache.get(cacheKey);
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.split("\\|", -1);
        if (parts.length < 15) {
            return null;
        }
        CalendarDay day = new CalendarDay();
        day.setTenantId(Long.valueOf(parts[0]));
        day.setCalendarId(Long.valueOf(parts[1]));
        day.setCalendarYear(Integer.valueOf(parts[2]));
        day.setCalendarDate(LocalDate.parse(parts[3]));
        day.setDayType(parts[4]);
        day.setWorkday(Integer.valueOf(parts[5]));
        day.setEnabled(Integer.valueOf(parts[6]));
        day.setDayOfWeek(Integer.valueOf(parts[7]));
        day.setDayName(nullIfBlank(parts[8]));
        day.setLunarYear(integerOrNull(parts[9]));
        day.setLunarMonth(integerOrNull(parts[10]));
        day.setLunarDay(integerOrNull(parts[11]));
        day.setLunarLeapMonth(integerOrNull(parts[12]));
        day.setLunarText(nullIfBlank(parts[13]));
        day.setGanzhiYear(nullIfBlank(parts[14]));
        day.setZodiac(parts.length > 15 ? nullIfBlank(parts[15]) : null);
        day.setSolarTerm(parts.length > 16 ? nullIfBlank(parts[16]) : null);
        return day;
    }

    private void writeDayCache(String cacheKey, CalendarDay day) {
        ICache cache = cacheProvider.getIfAvailable();
        if (cache == null) {
            return;
        }
        String value = day.getTenantId() + "|" + day.getCalendarId() + "|" + day.getCalendarYear() + "|"
                + day.getCalendarDate() + "|" + day.getDayType() + "|" + day.getWorkday() + "|" + day.getEnabled()
                + "|" + day.getDayOfWeek() + "|" + blankIfNull(day.getDayName())
                + "|" + blankIfNull(day.getLunarYear()) + "|" + blankIfNull(day.getLunarMonth())
                + "|" + blankIfNull(day.getLunarDay()) + "|" + blankIfNull(day.getLunarLeapMonth())
                + "|" + blankIfNull(day.getLunarText()) + "|" + blankIfNull(day.getGanzhiYear())
                + "|" + blankIfNull(day.getZodiac()) + "|" + blankIfNull(day.getSolarTerm());
        cache.set(cacheKey, value, kvProperties.getDayCacheTtlSeconds());
    }

    private String dayCacheKey(Long tenantId, Long calendarId, LocalDate date) {
        return "calendar:day:" + tenantId + ":" + calendarId + ":" + date;
    }

    private CalendarDayVO toDayVO(CalendarDay entity, Calendar calendar) {
        CalendarDayVO vo = new CalendarDayVO();
        vo.setId(entity.getId());
        if (calendar != null) {
            vo.setCalendarCode(calendar.getCalendarCode());
            vo.setCalendarName(calendar.getCalendarName());
        }
        vo.setCalendarYear(entity.getCalendarYear());
        vo.setDate(entity.getCalendarDate());
        vo.setDayOfWeek(entity.getDayOfWeek());
        vo.setDayType(CalendarDayTypes.normalize(entity.getDayType()));
        vo.setWorkday(entity.getWorkday() == 1);
        vo.setDayName(entity.getDayName());
        vo.setLunarYear(entity.getLunarYear());
        vo.setLunarMonth(entity.getLunarMonth());
        vo.setLunarDay(entity.getLunarDay());
        vo.setLunarLeapMonth(entity.getLunarLeapMonth() != null && entity.getLunarLeapMonth() == 1);
        vo.setLunarText(entity.getLunarText());
        vo.setGanzhiYear(entity.getGanzhiYear());
        vo.setZodiac(entity.getZodiac());
        vo.setSolarTerm(entity.getSolarTerm());
        vo.setSource(entity.getSource());
        vo.setRemark(entity.getRemark());
        vo.setEnabled(entity.getEnabled());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private String blankIfNull(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Integer integerOrNull(String value) {
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }
}
