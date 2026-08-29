package io.mango.calendar.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.calendar.api.command.BatchUpdateCalendarDaysCommand;
import io.mango.calendar.api.command.CreateCalendarCommand;
import io.mango.calendar.api.command.ImportCalendarDaysCommand;
import io.mango.calendar.api.command.ImportCalendarDayCommand;
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
import io.mango.calendar.core.entity.CalendarEntity;
import io.mango.calendar.core.entity.CalendarDayEntity;
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
import java.time.Month;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;

import static io.mango.calendar.api.enums.CalendarCode.CALENDAR_BUSINESS_ERROR;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(CalendarKvProperties.class)
public class CalendarAdminService implements ICalendarAdminService {

    private static final String SOURCE_DEFAULT = "系统默认";
    private static final String SOURCE_COPY = "年度复制";

    private final CalendarMapper calendarMapper;
    private final CalendarDayMapper dayMapper;
    private final ICalendarLunarService lunarService;
    private final ObjectProvider<ICache> cacheProvider;

    @Override
    public PageResult<CalendarVO> pageCalendars(CalendarPageQuery query) {
        CalendarPageQuery resolved = query == null ? new CalendarPageQuery() : query;
        IPage<CalendarEntity> page = calendarMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), calendarWrapper(resolved));
        return PageResult.of(page.getRecords().stream().map(this::toCalendarVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<CalendarOptionVO> listCalendarOptions(CalendarOptionQuery query) {
        CalendarOptionQuery resolved = query == null ? new CalendarOptionQuery() : query;
        LambdaQueryWrapper<CalendarEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CalendarEntity::getTenantId, CalendarSupport.currentTenantId());
        if (!Boolean.TRUE.equals(resolved.getIncludeDisabled())) {
            wrapper.eq(CalendarEntity::getStatus, 1);
        }
        String keyword = CalendarSupport.trimToNull(resolved.getKeyword());
        wrapper.and(StringUtils.hasText(keyword), nested -> nested
                .like(CalendarEntity::getCalendarCode, keyword)
                .or()
                .like(CalendarEntity::getCalendarName, keyword));
        wrapper.orderByAsc(CalendarEntity::getCalendarCode);
        return calendarMapper.selectList(wrapper).stream().map(this::toOptionVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCalendar(CreateCalendarCommand command) {
        String tenantId = CalendarSupport.currentTenantId();
        String calendarCode = CalendarSupport.trimRequired(command.getCalendarCode(), "日历编码不能为空");
        Require.isTrue(calendarMapper.selectByCode(tenantId, calendarCode) == null,
                CALENDAR_BUSINESS_ERROR, "日历编码已存在");
        CalendarEntity entity = new CalendarEntity();
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
        CalendarEntity entity = selectCalendarRequired(command.getId());
        String calendarCode = CalendarSupport.trimRequired(command.getCalendarCode(), "日历编码不能为空");
        CalendarEntity exists = calendarMapper.selectByCode(entity.getTenantId(), calendarCode);
        Require.isTrue(exists == null || exists.getId().equals(entity.getId()),
                CALENDAR_BUSINESS_ERROR, "日历编码已存在");
        entity.setCalendarCode(calendarCode);
        entity.setCalendarName(CalendarSupport.trimRequired(command.getCalendarName(), "日历名称不能为空"));
        return calendarMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCalendarStatus(UpdateCalendarStatusCommand command) {
        Require.notNull(command, CALENDAR_BUSINESS_ERROR, "更新日历状态命令不能为空");
        CalendarEntity entity = selectCalendarRequired(command.getId());
        entity.setStatus(command.getStatus());
        return calendarMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCalendar(Long id) {
        Require.notNull(id, CALENDAR_BUSINESS_ERROR, "日历 ID 不能为空");
        CalendarEntity calendar = selectCalendarRequired(id);
        String tenantId = CalendarSupport.currentTenantId();
        List<CalendarDayEntity> days = dayMapper.selectList(new LambdaQueryWrapper<CalendarDayEntity>()
                .eq(CalendarDayEntity::getTenantId, tenantId)
                .eq(CalendarDayEntity::getCalendarId, calendar.getId()));
        days.forEach(this::evictDay);
        dayMapper.delete(new LambdaQueryWrapper<CalendarDayEntity>()
                .eq(CalendarDayEntity::getTenantId, tenantId)
                .eq(CalendarDayEntity::getCalendarId, calendar.getId()));
        return calendarMapper.deleteById(calendar.getId()) > 0;
    }

    @Override
    public PageResult<CalendarYearSummaryVO> pageCalendarYears(CalendarYearPageQuery query) {
        CalendarYearPageQuery resolved = query == null ? new CalendarYearPageQuery() : query;
        String tenantId = CalendarSupport.currentTenantId();
        LambdaQueryWrapper<CalendarDayEntity> wrapper = yearWrapper(tenantId, resolved);
        IPage<CalendarDayEntity> page = dayMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), wrapper);
        Map<Long, CalendarEntity> calendars = calendarsById(tenantId);
        List<CalendarYearSummaryVO> records = page.getRecords().stream()
                .map(row -> yearSummary(tenantId, calendars.get(row.getCalendarId()), row.getCalendarYear()))
                .toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean initCalendarYear(InitCalendarYearCommand command) {
        return initCalendarYear(command, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean initResourceCalendarYear(InitCalendarYearCommand command,
                                            ToLongFunction<LocalDate> targetIdProvider) {
        Require.notNull(targetIdProvider, CALENDAR_BUSINESS_ERROR, "Resource 日历日期 ID 提供器不能为空");
        return initCalendarYear(command, targetIdProvider);
    }

    private boolean initCalendarYear(InitCalendarYearCommand command,
                                     ToLongFunction<LocalDate> targetIdProvider) {
        String tenantId = CalendarSupport.currentTenantId();
        CalendarEntity calendar = selectCalendarByCodeRequired(tenantId, command.getCalendarCode());
        long exists = dayMapper.countByYear(tenantId, calendar.getId(), command.getYear());
        boolean overwrite = Boolean.TRUE.equals(command.getOverwrite());
        Require.isTrue(overwrite || exists == 0, CALENDAR_BUSINESS_ERROR, "年度日历已存在");
        if (overwrite && exists > 0) {
            dayMapper.delete(yearDeleteWrapper(tenantId, calendar.getId(), command.getYear()));
        }
        Map<String, CalendarDayEntity> sourceByMonthDay = sourceDaysByMonthDay(tenantId, calendar, command.getSourceYear());
        Year targetYear = Year.of(command.getYear());
        LocalDate date = targetYear.atDay(1);
        LocalDate endDate = targetYear.atMonth(Month.DECEMBER).atEndOfMonth();
        while (!date.isAfter(endDate)) {
            CalendarDayEntity entity = defaultDay(tenantId, calendar.getId(), date);
            if (targetIdProvider != null) {
                entity.setId(targetIdProvider.applyAsLong(date));
            }
            CalendarDayEntity source = sourceByMonthDay.get(monthDayKey(date));
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
        String tenantId = CalendarSupport.currentTenantId();
        CalendarEntity calendar = selectCalendarByCodeRequired(tenantId, command.getCalendarCode());
        List<CalendarDayEntity> days = dayMapper.selectByYear(tenantId, calendar.getId(), command.getYear());
        Require.isTrue(!days.isEmpty(), CALENDAR_BUSINESS_ERROR, "年度日历未初始化");
        for (CalendarDayEntity day : days) {
            lunarService.applyLunarInfo(day);
            dayMapper.updateById(day);
            evictDay(day);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCalendarYearEnabled(UpdateCalendarYearEnabledCommand command) {
        String tenantId = CalendarSupport.currentTenantId();
        CalendarEntity calendar = selectCalendarByCodeRequired(tenantId, command.getCalendarCode());
        Require.isTrue(dayMapper.countByYear(tenantId, calendar.getId(), command.getYear()) > 0,
                CALENDAR_BUSINESS_ERROR, "年度日历未初始化");
        CalendarDayEntity entity = new CalendarDayEntity();
        entity.setEnabled(command.getEnabled());
        int updated = dayMapper.update(entity, yearDeleteWrapper(tenantId, calendar.getId(), command.getYear()));
        return updated > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCalendarYear(String calendarCode, Integer year) {
        String tenantId = CalendarSupport.currentTenantId();
        CalendarEntity calendar = selectCalendarByCodeRequired(tenantId, calendarCode);
        List<CalendarDayEntity> days = dayMapper.selectByYear(tenantId, calendar.getId(), year);
        Require.isTrue(!days.isEmpty(), CALENDAR_BUSINESS_ERROR, "年度日历未初始化");
        days.forEach(this::evictDay);
        return dayMapper.delete(yearDeleteWrapper(tenantId, calendar.getId(), year)) > 0;
    }

    @Override
    public CalendarYearSummaryVO yearSummary(CalendarYearSummaryQuery query) {
        String tenantId = CalendarSupport.currentTenantId();
        CalendarEntity calendar = selectCalendarByCodeRequired(tenantId, query.getCalendarCode());
        return yearSummary(tenantId, calendar, query.getYear());
    }

    @Override
    public PageResult<CalendarDayVO> pageCalendarDays(CalendarDayPageQuery query) {
        CalendarDayPageQuery resolved = query == null ? new CalendarDayPageQuery() : query;
        String tenantId = CalendarSupport.currentTenantId();
        CalendarEntity calendar = StringUtils.hasText(resolved.getCalendarCode())
                ? selectCalendarByCodeRequired(tenantId, resolved.getCalendarCode())
                : null;
        IPage<CalendarDayEntity> page = dayMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()),
                dayWrapper(tenantId, calendar, resolved));
        Map<Long, CalendarEntity> calendars = calendarsById(tenantId);
        return PageResult.of(page.getRecords().stream()
                .map(day -> toDayVO(day, calendars.get(day.getCalendarId())))
                .toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCalendarDay(UpdateCalendarDayCommand command) {
        Require.notNull(command, CALENDAR_BUSINESS_ERROR, "更新日历日期命令不能为空");
        CalendarDayEntity entity = selectDayRequired(command.getId());
        applyDayUpdate(entity, command.getDayType(), command.getDayName(), command.getSource(), command.getRemark());
        evictDay(entity);
        return dayMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCalendarDay(Long id) {
        Require.notNull(id, CALENDAR_BUSINESS_ERROR, "日期 ID 不能为空");
        CalendarDayEntity entity = selectDayRequired(id);
        evictDay(entity);
        return dayMapper.deleteById(entity.getId()) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateCalendarDays(BatchUpdateCalendarDaysCommand command) {
        Require.notNull(command, CALENDAR_BUSINESS_ERROR, "批量更新日历日期命令不能为空");
        for (Long id : command.getIds()) {
            CalendarDayEntity entity = selectDayRequired(id);
            applyDayUpdate(entity, command.getDayType(), command.getDayName(), command.getSource(), command.getRemark());
            dayMapper.updateById(entity);
            evictDay(entity);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean importCalendarDays(ImportCalendarDaysCommand command) {
        String tenantId = CalendarSupport.currentTenantId();
        CalendarEntity calendar = selectCalendarByCodeRequired(tenantId, command.getCalendarCode());
        Require.isTrue(dayMapper.countByYear(tenantId, calendar.getId(), command.getYear()) > 0,
                CALENDAR_BUSINESS_ERROR, "年度日历未初始化");
        for (ImportCalendarDayCommand item : command.getItems()) {
            Require.isTrue(item.getDate().getYear() == command.getYear(), CALENDAR_BUSINESS_ERROR,
                    "导入日期必须属于指定年度");
            CalendarDayEntity entity = dayMapper.selectByDate(tenantId, calendar.getId(), item.getDate());
            Require.notNull(entity, CALENDAR_BUSINESS_ERROR,
                    "年度日历未初始化：" + command.getCalendarCode() + " " + command.getYear());
            applyDayUpdate(entity, item.getDayType(), item.getDayName(), item.getSource(), item.getRemark());
            dayMapper.updateById(entity);
            evictDay(entity);
        }
        return true;
    }

    private LambdaQueryWrapper<CalendarEntity> calendarWrapper(CalendarPageQuery query) {
        LambdaQueryWrapper<CalendarEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CalendarEntity::getTenantId, CalendarSupport.currentTenantId());
        String keyword = CalendarSupport.trimToNull(query.getKeyword());
        wrapper.and(StringUtils.hasText(keyword), nested -> nested
                .like(CalendarEntity::getCalendarCode, keyword)
                .or()
                .like(CalendarEntity::getCalendarName, keyword));
        wrapper.eq(query.getStatus() != null, CalendarEntity::getStatus, query.getStatus());
        wrapper.orderByDesc(CalendarEntity::getUpdatedAt);
        return wrapper;
    }

    private LambdaQueryWrapper<CalendarDayEntity> yearWrapper(String tenantId, CalendarYearPageQuery query) {
        LambdaQueryWrapper<CalendarDayEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(CalendarDayEntity::getCalendarId, CalendarDayEntity::getCalendarYear);
        wrapper.eq(CalendarDayEntity::getTenantId, tenantId);
        if (StringUtils.hasText(query.getCalendarCode())) {
            wrapper.eq(CalendarDayEntity::getCalendarId, selectCalendarByCodeRequired(tenantId, query.getCalendarCode()).getId());
        }
        wrapper.eq(query.getYear() != null, CalendarDayEntity::getCalendarYear, query.getYear());
        wrapper.eq(query.getEnabled() != null, CalendarDayEntity::getEnabled, query.getEnabled());
        wrapper.groupBy(CalendarDayEntity::getCalendarId, CalendarDayEntity::getCalendarYear);
        wrapper.orderByDesc(CalendarDayEntity::getCalendarYear);
        return wrapper;
    }

    private LambdaQueryWrapper<CalendarDayEntity> dayWrapper(String tenantId, CalendarEntity calendar, CalendarDayPageQuery query) {
        LambdaQueryWrapper<CalendarDayEntity> wrapper = new LambdaQueryWrapper<>();
        LocalDate startDate = optionalDate(query.getStartDate());
        LocalDate endDate = optionalDate(query.getEndDate());
        CalendarDayType dayType = CalendarDayTypes.normalize(query.getDayType());
        Boolean workday = optionalBoolean(query.getWorkday());
        wrapper.eq(CalendarDayEntity::getTenantId, tenantId);
        wrapper.eq(calendar != null, CalendarDayEntity::getCalendarId, calendar == null ? null : calendar.getId());
        wrapper.eq(query.getYear() != null, CalendarDayEntity::getCalendarYear, query.getYear());
        wrapper.ge(startDate != null, CalendarDayEntity::getCalendarDate, startDate);
        wrapper.le(endDate != null, CalendarDayEntity::getCalendarDate, endDate);
        wrapper.eq(dayType != null, CalendarDayEntity::getDayType, dayType == null ? null : dayType.name());
        wrapper.eq(workday != null, CalendarDayEntity::getWorkday, Boolean.TRUE.equals(workday) ? 1 : 0);
        wrapper.eq(query.getEnabled() != null, CalendarDayEntity::getEnabled, query.getEnabled());
        String keyword = CalendarSupport.trimToNull(query.getKeyword());
        wrapper.and(StringUtils.hasText(keyword), nested -> nested
                .like(CalendarDayEntity::getDayName, keyword)
                .or()
                .like(CalendarDayEntity::getSource, keyword)
                .or()
                .like(CalendarDayEntity::getRemark, keyword));
        wrapper.orderByAsc(CalendarDayEntity::getCalendarDate);
        return wrapper;
    }

    private LocalDate optionalDate(String value) {
        String normalized = CalendarSupport.trimToNull(value);
        return normalized == null ? null : LocalDate.parse(normalized);
    }

    private Boolean optionalBoolean(String value) {
        String normalized = CalendarSupport.trimToNull(value);
        return normalized == null ? null : Boolean.valueOf(normalized);
    }

    private LambdaQueryWrapper<CalendarDayEntity> yearDeleteWrapper(String tenantId, Long calendarId, Integer year) {
        LambdaQueryWrapper<CalendarDayEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CalendarDayEntity::getTenantId, tenantId);
        wrapper.eq(CalendarDayEntity::getCalendarId, calendarId);
        wrapper.eq(CalendarDayEntity::getCalendarYear, year);
        return wrapper;
    }

    private CalendarEntity selectCalendarRequired(Long id) {
        Require.notNull(id, CALENDAR_BUSINESS_ERROR, "日历 ID 不能为空");
        CalendarEntity entity = calendarMapper.selectById(id);
        Require.notNull(entity, CALENDAR_BUSINESS_ERROR, "日历不存在");
        Require.isTrue(CalendarSupport.currentTenantId().equals(entity.getTenantId()),
                CALENDAR_BUSINESS_ERROR, "日历不存在");
        return entity;
    }

    private CalendarEntity selectCalendarByCodeRequired(String tenantId, String calendarCode) {
        String code = CalendarSupport.trimRequired(calendarCode, "日历编码不能为空");
        CalendarEntity calendar = calendarMapper.selectByCode(tenantId, code);
        Require.notNull(calendar, CALENDAR_BUSINESS_ERROR, "日历不存在：" + code);
        return calendar;
    }

    private CalendarDayEntity selectDayRequired(Long id) {
        Require.notNull(id, CALENDAR_BUSINESS_ERROR, "日期 ID 不能为空");
        CalendarDayEntity entity = dayMapper.selectById(id);
        Require.notNull(entity, CALENDAR_BUSINESS_ERROR, "日历日期不存在");
        Require.isTrue(CalendarSupport.currentTenantId().equals(entity.getTenantId()),
                CALENDAR_BUSINESS_ERROR, "日历日期不存在");
        return entity;
    }

    private CalendarYearSummaryVO yearSummary(String tenantId, CalendarEntity calendar, Integer year) {
        Require.notNull(calendar, CALENDAR_BUSINESS_ERROR, "日历不存在");
        List<CalendarDayEntity> days = dayMapper.selectByYear(tenantId, calendar.getId(), year);
        Require.isTrue(!days.isEmpty(), CALENDAR_BUSINESS_ERROR, "年度日历未初始化");
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

    private int countType(List<CalendarDayEntity> days, CalendarDayType dayType) {
        return (int) days.stream()
                .filter(day -> CalendarDayTypes.normalize(day.getDayType()) == dayType)
                .count();
    }

    private Map<Long, CalendarEntity> calendarsById(String tenantId) {
        LambdaQueryWrapper<CalendarEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CalendarEntity::getTenantId, tenantId);
        List<CalendarEntity> calendars = calendarMapper.selectList(wrapper);
        Map<Long, CalendarEntity> result = new HashMap<>(calendars.size());
        for (CalendarEntity calendar : calendars) {
            result.put(calendar.getId(), calendar);
        }
        return result;
    }

    private Map<String, CalendarDayEntity> sourceDaysByMonthDay(String tenantId, CalendarEntity calendar, Integer sourceYear) {
        if (sourceYear == null) {
            return Map.of();
        }
        List<CalendarDayEntity> sourceDays = dayMapper.selectByYear(tenantId, calendar.getId(), sourceYear);
        Require.isTrue(!sourceDays.isEmpty(), CALENDAR_BUSINESS_ERROR, "复制来源年度未初始化");
        Map<String, CalendarDayEntity> result = new HashMap<>(sourceDays.size());
        for (CalendarDayEntity day : sourceDays) {
            result.put(monthDayKey(day.getCalendarDate()), day);
        }
        return result;
    }

    private String monthDayKey(LocalDate date) {
        return date.getMonthValue() + "-" + date.getDayOfMonth();
    }

    private CalendarDayEntity defaultDay(String tenantId, Long calendarId, LocalDate date) {
        CalendarDayEntity entity = new CalendarDayEntity();
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

    private void copySourceDay(CalendarDayEntity source, CalendarDayEntity target) {
        CalendarDayType dayType = CalendarDayTypes.normalize(source.getDayType());
        target.setDayType(dayType.name());
        target.setWorkday(CalendarDayTypes.isWorkday(dayType) ? 1 : 0);
        target.setDayName(source.getDayName());
        target.setSource(SOURCE_COPY);
        target.setRemark(source.getRemark());
        target.setEnabled(source.getEnabled());
    }

    private void applyDayUpdate(CalendarDayEntity entity, CalendarDayType dayType, String dayName, String source, String remark) {
        CalendarDayType normalized = CalendarDayTypes.normalize(dayType);
        entity.setDayType(normalized.name());
        entity.setWorkday(CalendarDayTypes.isWorkday(normalized) ? 1 : 0);
        entity.setDayName(CalendarSupport.trimToNull(dayName));
        entity.setSource(CalendarSupport.trimToNull(source));
        entity.setRemark(CalendarSupport.trimToNull(remark));
    }

    private void evictDay(CalendarDayEntity day) {
        ICache cache = cacheProvider.getIfAvailable();
        if (cache == null) {
            return;
        }
        cache.delete("calendar:day:" + day.getTenantId() + ":" + day.getCalendarId() + ":" + day.getCalendarDate());
    }

    private CalendarVO toCalendarVO(CalendarEntity entity) {
        CalendarVO vo = new CalendarVO();
        vo.setId(entity.getId());
        vo.setCalendarCode(entity.getCalendarCode());
        vo.setCalendarName(entity.getCalendarName());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }

    private CalendarOptionVO toOptionVO(CalendarEntity entity) {
        CalendarOptionVO vo = new CalendarOptionVO();
        vo.setCalendarCode(entity.getCalendarCode());
        vo.setCalendarName(entity.getCalendarName());
        vo.setStatus(entity.getStatus());
        return vo;
    }

    private CalendarDayVO toDayVO(CalendarDayEntity entity, CalendarEntity calendar) {
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
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }
}
