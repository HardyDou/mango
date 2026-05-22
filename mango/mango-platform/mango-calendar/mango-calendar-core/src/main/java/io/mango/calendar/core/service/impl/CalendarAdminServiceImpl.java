package io.mango.calendar.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.calendar.api.command.BatchUpdateCalendarDaysCommand;
import io.mango.calendar.api.command.CreateCalendarCommand;
import io.mango.calendar.api.command.ImportCalendarDaysCommand;
import io.mango.calendar.api.command.InitCalendarYearCommand;
import io.mango.calendar.api.command.RefreshCalendarYearLunarCommand;
import io.mango.calendar.api.command.UpdateCalendarCommand;
import io.mango.calendar.api.command.UpdateCalendarDayCommand;
import io.mango.calendar.api.command.UpdateCalendarStatusCommand;
import io.mango.calendar.api.command.UpdateCalendarYearEnabledCommand;
import io.mango.calendar.api.enums.CalendarDayType;
import io.mango.calendar.api.query.CalendarDayPageQuery;
import io.mango.calendar.api.query.CalendarOptionQuery;
import io.mango.calendar.api.query.CalendarPageQuery;
import io.mango.calendar.api.query.CalendarYearPageQuery;
import io.mango.calendar.api.query.CalendarYearSummaryQuery;
import io.mango.calendar.api.vo.CalendarDayVO;
import io.mango.calendar.api.vo.CalendarOptionVO;
import io.mango.calendar.api.vo.CalendarVO;
import io.mango.calendar.api.vo.CalendarYearSummaryVO;
import io.mango.calendar.core.config.CalendarKvProperties;
import io.mango.calendar.core.entity.Calendar;
import io.mango.calendar.core.entity.CalendarDay;
import io.mango.calendar.core.mapper.CalendarDayMapper;
import io.mango.calendar.core.mapper.CalendarMapper;
import io.mango.calendar.core.service.ICalendarAdminService;
import io.mango.calendar.core.service.ICalendarLunarService;
import io.mango.calendar.core.support.CalendarDayTypes;
import io.mango.calendar.core.support.CalendarSupport;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.kv.api.ICache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(CalendarKvProperties.class)
public class CalendarAdminServiceImpl implements ICalendarAdminService {

    private static final String SOURCE_DEFAULT = "系统默认";
    private static final String SOURCE_COPY = "年度复制";

    private final CalendarMapper calendarMapper;
    private final CalendarDayMapper dayMapper;
    private final ICalendarLunarService lunarService;
    private final ObjectProvider<ICache> cacheProvider;

    @Override
    public PageResult<CalendarVO> pageCalendars(CalendarPageQuery query) {
        CalendarPageQuery resolved = query == null ? new CalendarPageQuery() : query;
        IPage<Calendar> page = calendarMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), calendarWrapper(resolved));
        return PageResult.of(page.getRecords().stream().map(this::toCalendarVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<CalendarOptionVO> listCalendarOptions(CalendarOptionQuery query) {
        CalendarOptionQuery resolved = query == null ? new CalendarOptionQuery() : query;
        LambdaQueryWrapper<Calendar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Calendar::getTenantId, CalendarSupport.currentTenantId());
        if (!Boolean.TRUE.equals(resolved.getIncludeDisabled())) {
            wrapper.eq(Calendar::getStatus, 1);
        }
        String keyword = CalendarSupport.trimToNull(resolved.getKeyword());
        wrapper.and(StringUtils.hasText(keyword), nested -> nested
                .like(Calendar::getCalendarCode, keyword)
                .or()
                .like(Calendar::getCalendarName, keyword));
        wrapper.orderByAsc(Calendar::getCalendarCode);
        return calendarMapper.selectList(wrapper).stream().map(this::toOptionVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCalendar(CreateCalendarCommand command) {
        Long tenantId = CalendarSupport.currentTenantId();
        String calendarCode = CalendarSupport.trimRequired(command.getCalendarCode(), "日历编码不能为空");
        Require.isNull(calendarMapper.selectByCode(tenantId, calendarCode), "日历编码已存在");
        Calendar entity = new Calendar();
        entity.setTenantId(tenantId);
        entity.setCalendarCode(calendarCode);
        entity.setCalendarName(CalendarSupport.trimRequired(command.getCalendarName(), "日历名称不能为空"));
        entity.setStatus(1);
        calendarMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCalendar(UpdateCalendarCommand command) {
        Calendar entity = selectCalendarRequired(command.getId());
        String calendarCode = CalendarSupport.trimRequired(command.getCalendarCode(), "日历编码不能为空");
        Calendar exists = calendarMapper.selectByCode(entity.getTenantId(), calendarCode);
        Require.isTrue(exists == null || exists.getId().equals(entity.getId()), "日历编码已存在");
        entity.setCalendarCode(calendarCode);
        entity.setCalendarName(CalendarSupport.trimRequired(command.getCalendarName(), "日历名称不能为空"));
        return calendarMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCalendarStatus(UpdateCalendarStatusCommand command) {
        Calendar entity = selectCalendarRequired(command.getId());
        entity.setStatus(command.getStatus());
        return calendarMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCalendar(Long id) {
        Calendar calendar = selectCalendarRequired(id);
        Long tenantId = CalendarSupport.currentTenantId();
        List<CalendarDay> days = dayMapper.selectList(new LambdaQueryWrapper<CalendarDay>()
                .eq(CalendarDay::getTenantId, tenantId)
                .eq(CalendarDay::getCalendarId, calendar.getId()));
        days.forEach(this::evictDay);
        dayMapper.delete(new LambdaQueryWrapper<CalendarDay>()
                .eq(CalendarDay::getTenantId, tenantId)
                .eq(CalendarDay::getCalendarId, calendar.getId()));
        return calendarMapper.deleteById(calendar.getId()) > 0;
    }

    @Override
    public PageResult<CalendarYearSummaryVO> pageCalendarYears(CalendarYearPageQuery query) {
        CalendarYearPageQuery resolved = query == null ? new CalendarYearPageQuery() : query;
        Long tenantId = CalendarSupport.currentTenantId();
        LambdaQueryWrapper<CalendarDay> wrapper = yearWrapper(tenantId, resolved);
        IPage<CalendarDay> page = dayMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), wrapper);
        Map<Long, Calendar> calendars = calendarsById(tenantId);
        List<CalendarYearSummaryVO> records = page.getRecords().stream()
                .map(row -> yearSummary(tenantId, calendars.get(row.getCalendarId()), row.getCalendarYear()))
                .toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean initCalendarYear(InitCalendarYearCommand command) {
        Long tenantId = CalendarSupport.currentTenantId();
        Calendar calendar = selectCalendarByCodeRequired(tenantId, command.getCalendarCode());
        long exists = dayMapper.countByYear(tenantId, calendar.getId(), command.getYear());
        boolean overwrite = Boolean.TRUE.equals(command.getOverwrite());
        Require.isTrue(overwrite || exists == 0, "年度日历已存在");
        if (overwrite && exists > 0) {
            dayMapper.delete(yearDeleteWrapper(tenantId, calendar.getId(), command.getYear()));
        }
        Map<String, CalendarDay> sourceByMonthDay = sourceDaysByMonthDay(tenantId, calendar, command.getSourceYear());
        LocalDate date = LocalDate.of(command.getYear(), 1, 1);
        LocalDate endDate = LocalDate.of(command.getYear(), 12, 31);
        while (!date.isAfter(endDate)) {
            CalendarDay entity = defaultDay(tenantId, calendar.getId(), date);
            CalendarDay source = sourceByMonthDay.get(monthDayKey(date));
            if (source != null && !CalendarDayTypes.isDefaultType(CalendarDayTypes.normalize(source.getDayType()))) {
                copySourceDay(source, entity);
            }
            lunarService.applyLunarInfo(entity);
            dayMapper.insert(entity);
            date = date.plusDays(1);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refreshCalendarYearLunar(RefreshCalendarYearLunarCommand command) {
        Long tenantId = CalendarSupport.currentTenantId();
        Calendar calendar = selectCalendarByCodeRequired(tenantId, command.getCalendarCode());
        List<CalendarDay> days = dayMapper.selectByYear(tenantId, calendar.getId(), command.getYear());
        Require.isTrue(!days.isEmpty(), "年度日历未初始化");
        for (CalendarDay day : days) {
            lunarService.applyLunarInfo(day);
            dayMapper.updateById(day);
            evictDay(day);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCalendarYearEnabled(UpdateCalendarYearEnabledCommand command) {
        Long tenantId = CalendarSupport.currentTenantId();
        Calendar calendar = selectCalendarByCodeRequired(tenantId, command.getCalendarCode());
        Require.isTrue(dayMapper.countByYear(tenantId, calendar.getId(), command.getYear()) > 0,
                "年度日历未初始化");
        CalendarDay entity = new CalendarDay();
        entity.setEnabled(command.getEnabled());
        int updated = dayMapper.update(entity, yearDeleteWrapper(tenantId, calendar.getId(), command.getYear()));
        return updated > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCalendarYear(String calendarCode, Integer year) {
        Long tenantId = CalendarSupport.currentTenantId();
        Calendar calendar = selectCalendarByCodeRequired(tenantId, calendarCode);
        List<CalendarDay> days = dayMapper.selectByYear(tenantId, calendar.getId(), year);
        Require.isTrue(!days.isEmpty(), "年度日历未初始化");
        days.forEach(this::evictDay);
        return dayMapper.delete(yearDeleteWrapper(tenantId, calendar.getId(), year)) > 0;
    }

    @Override
    public CalendarYearSummaryVO yearSummary(CalendarYearSummaryQuery query) {
        Long tenantId = CalendarSupport.currentTenantId();
        Calendar calendar = selectCalendarByCodeRequired(tenantId, query.getCalendarCode());
        return yearSummary(tenantId, calendar, query.getYear());
    }

    @Override
    public PageResult<CalendarDayVO> pageCalendarDays(CalendarDayPageQuery query) {
        CalendarDayPageQuery resolved = query == null ? new CalendarDayPageQuery() : query;
        Long tenantId = CalendarSupport.currentTenantId();
        Calendar calendar = StringUtils.hasText(resolved.getCalendarCode())
                ? selectCalendarByCodeRequired(tenantId, resolved.getCalendarCode())
                : null;
        IPage<CalendarDay> page = dayMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                dayWrapper(tenantId, calendar, resolved));
        Map<Long, Calendar> calendars = calendarsById(tenantId);
        return PageResult.of(page.getRecords().stream()
                .map(day -> toDayVO(day, calendars.get(day.getCalendarId())))
                .toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCalendarDay(UpdateCalendarDayCommand command) {
        CalendarDay entity = selectDayRequired(command.getId());
        applyDayUpdate(entity, command.getDayType(), command.getDayName(), command.getSource(), command.getRemark());
        evictDay(entity);
        return dayMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCalendarDay(Long id) {
        CalendarDay entity = selectDayRequired(id);
        evictDay(entity);
        return dayMapper.deleteById(entity.getId()) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateCalendarDays(BatchUpdateCalendarDaysCommand command) {
        for (Long id : command.getIds()) {
            CalendarDay entity = selectDayRequired(id);
            applyDayUpdate(entity, command.getDayType(), command.getDayName(), command.getSource(), command.getRemark());
            dayMapper.updateById(entity);
            evictDay(entity);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean importCalendarDays(ImportCalendarDaysCommand command) {
        Long tenantId = CalendarSupport.currentTenantId();
        Calendar calendar = selectCalendarByCodeRequired(tenantId, command.getCalendarCode());
        Require.isTrue(dayMapper.countByYear(tenantId, calendar.getId(), command.getYear()) > 0, "年度日历未初始化");
        for (ImportCalendarDaysCommand.Item item : command.getItems()) {
            Require.isTrue(item.getDate().getYear() == command.getYear(), "导入日期必须属于指定年度");
            CalendarDay entity = dayMapper.selectByDate(tenantId, calendar.getId(), item.getDate());
            Require.notNull(entity, "年度日历未初始化：" + command.getCalendarCode() + " " + command.getYear());
            applyDayUpdate(entity, item.getDayType(), item.getDayName(), item.getSource(), item.getRemark());
            dayMapper.updateById(entity);
            evictDay(entity);
        }
        return true;
    }

    private LambdaQueryWrapper<Calendar> calendarWrapper(CalendarPageQuery query) {
        LambdaQueryWrapper<Calendar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Calendar::getTenantId, CalendarSupport.currentTenantId());
        String keyword = CalendarSupport.trimToNull(query.getKeyword());
        wrapper.and(StringUtils.hasText(keyword), nested -> nested
                .like(Calendar::getCalendarCode, keyword)
                .or()
                .like(Calendar::getCalendarName, keyword));
        wrapper.eq(query.getStatus() != null, Calendar::getStatus, query.getStatus());
        wrapper.orderByDesc(Calendar::getUpdateTime);
        return wrapper;
    }

    private LambdaQueryWrapper<CalendarDay> yearWrapper(Long tenantId, CalendarYearPageQuery query) {
        LambdaQueryWrapper<CalendarDay> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(CalendarDay::getCalendarId, CalendarDay::getCalendarYear);
        wrapper.eq(CalendarDay::getTenantId, tenantId);
        if (StringUtils.hasText(query.getCalendarCode())) {
            wrapper.eq(CalendarDay::getCalendarId, selectCalendarByCodeRequired(tenantId, query.getCalendarCode()).getId());
        }
        wrapper.eq(query.getYear() != null, CalendarDay::getCalendarYear, query.getYear());
        wrapper.eq(query.getEnabled() != null, CalendarDay::getEnabled, query.getEnabled());
        wrapper.groupBy(CalendarDay::getCalendarId, CalendarDay::getCalendarYear);
        wrapper.orderByDesc(CalendarDay::getCalendarYear);
        return wrapper;
    }

    private LambdaQueryWrapper<CalendarDay> dayWrapper(Long tenantId, Calendar calendar, CalendarDayPageQuery query) {
        LambdaQueryWrapper<CalendarDay> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CalendarDay::getTenantId, tenantId);
        wrapper.eq(calendar != null, CalendarDay::getCalendarId, calendar == null ? null : calendar.getId());
        wrapper.eq(query.getYear() != null, CalendarDay::getCalendarYear, query.getYear());
        wrapper.ge(query.getStartDate() != null, CalendarDay::getCalendarDate, query.getStartDate());
        wrapper.le(query.getEndDate() != null, CalendarDay::getCalendarDate, query.getEndDate());
        wrapper.eq(query.getDayType() != null, CalendarDay::getDayType,
                query.getDayType() == null ? null : CalendarDayTypes.normalize(query.getDayType()).name());
        wrapper.eq(query.getWorkday() != null, CalendarDay::getWorkday, Boolean.TRUE.equals(query.getWorkday()) ? 1 : 0);
        wrapper.eq(query.getEnabled() != null, CalendarDay::getEnabled, query.getEnabled());
        String keyword = CalendarSupport.trimToNull(query.getKeyword());
        wrapper.and(StringUtils.hasText(keyword), nested -> nested
                .like(CalendarDay::getDayName, keyword)
                .or()
                .like(CalendarDay::getSource, keyword)
                .or()
                .like(CalendarDay::getRemark, keyword));
        wrapper.orderByAsc(CalendarDay::getCalendarDate);
        return wrapper;
    }

    private LambdaQueryWrapper<CalendarDay> yearDeleteWrapper(Long tenantId, Long calendarId, Integer year) {
        LambdaQueryWrapper<CalendarDay> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CalendarDay::getTenantId, tenantId);
        wrapper.eq(CalendarDay::getCalendarId, calendarId);
        wrapper.eq(CalendarDay::getCalendarYear, year);
        return wrapper;
    }

    private Calendar selectCalendarRequired(Long id) {
        Require.notNull(id, "日历 ID 不能为空");
        Calendar entity = calendarMapper.selectById(id);
        Require.notNull(entity, "日历不存在");
        Require.isTrue(CalendarSupport.currentTenantId().equals(entity.getTenantId()), "日历不存在");
        return entity;
    }

    private Calendar selectCalendarByCodeRequired(Long tenantId, String calendarCode) {
        String code = CalendarSupport.trimRequired(calendarCode, "日历编码不能为空");
        Calendar calendar = calendarMapper.selectByCode(tenantId, code);
        Require.notNull(calendar, "日历不存在：" + code);
        return calendar;
    }

    private CalendarDay selectDayRequired(Long id) {
        Require.notNull(id, "日期 ID 不能为空");
        CalendarDay entity = dayMapper.selectById(id);
        Require.notNull(entity, "日历日期不存在");
        Require.isTrue(CalendarSupport.currentTenantId().equals(entity.getTenantId()), "日历日期不存在");
        return entity;
    }

    private CalendarYearSummaryVO yearSummary(Long tenantId, Calendar calendar, Integer year) {
        Require.notNull(calendar, "日历不存在");
        List<CalendarDay> days = dayMapper.selectByYear(tenantId, calendar.getId(), year);
        Require.isTrue(!days.isEmpty(), "年度日历未初始化");
        CalendarYearSummaryVO vo = new CalendarYearSummaryVO();
        vo.setCalendarCode(calendar.getCalendarCode());
        vo.setCalendarName(calendar.getCalendarName());
        vo.setYear(year);
        vo.setTotalDays(days.size());
        vo.setWorkdays((int) days.stream().filter(day -> day.getWorkday() == 1).count());
        vo.setRestdays(vo.getTotalDays() - vo.getWorkdays());
        vo.setLegalHolidays(countType(days, CalendarDayType.LEGAL_HOLIDAY));
        vo.setAdjustedWorkdays(countType(days, CalendarDayType.ADJUSTED_WORKDAY));
        vo.setTempClosedDays(countType(days, CalendarDayType.TEMP_CLOSED_DAY));
        vo.setTempOpenDays(countType(days, CalendarDayType.TEMP_OPEN_DAY));
        vo.setEnabled(days.stream().allMatch(day -> day.getEnabled() == 1) ? 1 : 0);
        return vo;
    }

    private int countType(List<CalendarDay> days, CalendarDayType dayType) {
        return (int) days.stream()
                .filter(day -> CalendarDayTypes.normalize(day.getDayType()) == dayType)
                .count();
    }

    private Map<Long, Calendar> calendarsById(Long tenantId) {
        LambdaQueryWrapper<Calendar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Calendar::getTenantId, tenantId);
        Map<Long, Calendar> result = new HashMap<>();
        for (Calendar calendar : calendarMapper.selectList(wrapper)) {
            result.put(calendar.getId(), calendar);
        }
        return result;
    }

    private Map<String, CalendarDay> sourceDaysByMonthDay(Long tenantId, Calendar calendar, Integer sourceYear) {
        if (sourceYear == null) {
            return Map.of();
        }
        List<CalendarDay> sourceDays = dayMapper.selectByYear(tenantId, calendar.getId(), sourceYear);
        Require.isTrue(!sourceDays.isEmpty(), "复制来源年度未初始化");
        Map<String, CalendarDay> result = new HashMap<>();
        for (CalendarDay day : sourceDays) {
            result.put(monthDayKey(day.getCalendarDate()), day);
        }
        return result;
    }

    private String monthDayKey(LocalDate date) {
        return date.getMonthValue() + "-" + date.getDayOfMonth();
    }

    private CalendarDay defaultDay(Long tenantId, Long calendarId, LocalDate date) {
        CalendarDay entity = new CalendarDay();
        entity.setTenantId(tenantId);
        entity.setCalendarId(calendarId);
        entity.setCalendarYear(date.getYear());
        entity.setCalendarDate(date);
        entity.setDayOfWeek(date.getDayOfWeek().getValue());
        CalendarDayType dayType = CalendarDayTypes.defaultType(date.getDayOfWeek());
        entity.setDayType(dayType.name());
        entity.setWorkday(CalendarDayTypes.isWorkday(dayType) ? 1 : 0);
        entity.setSource(SOURCE_DEFAULT);
        entity.setEnabled(1);
        return entity;
    }

    private void copySourceDay(CalendarDay source, CalendarDay target) {
        CalendarDayType dayType = CalendarDayTypes.normalize(source.getDayType());
        target.setDayType(dayType.name());
        target.setWorkday(CalendarDayTypes.isWorkday(dayType) ? 1 : 0);
        target.setDayName(source.getDayName());
        target.setSource(SOURCE_COPY);
        target.setRemark(source.getRemark());
        target.setEnabled(source.getEnabled());
    }

    private void applyDayUpdate(CalendarDay entity, CalendarDayType dayType, String dayName, String source, String remark) {
        CalendarDayType normalized = CalendarDayTypes.normalize(dayType);
        entity.setDayType(normalized.name());
        entity.setWorkday(CalendarDayTypes.isWorkday(normalized) ? 1 : 0);
        entity.setDayName(CalendarSupport.trimToNull(dayName));
        entity.setSource(CalendarSupport.trimToNull(source));
        entity.setRemark(CalendarSupport.trimToNull(remark));
    }

    private void evictDay(CalendarDay day) {
        ICache cache = cacheProvider.getIfAvailable();
        if (cache == null) {
            return;
        }
        cache.delete("calendar:day:" + day.getTenantId() + ":" + day.getCalendarId() + ":" + day.getCalendarDate());
    }

    private CalendarVO toCalendarVO(Calendar entity) {
        CalendarVO vo = new CalendarVO();
        vo.setId(entity.getId());
        vo.setCalendarCode(entity.getCalendarCode());
        vo.setCalendarName(entity.getCalendarName());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private CalendarOptionVO toOptionVO(Calendar entity) {
        CalendarOptionVO vo = new CalendarOptionVO();
        vo.setCalendarCode(entity.getCalendarCode());
        vo.setCalendarName(entity.getCalendarName());
        vo.setStatus(entity.getStatus());
        return vo;
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
}
