package io.mango.calendar.core.service.impl;

import io.mango.calendar.api.enums.CalendarDayType;
import io.mango.calendar.api.query.AddWorkdaysQuery;
import io.mango.calendar.api.query.BatchCheckWorkdayQuery;
import io.mango.calendar.api.query.CalendarDateQuery;
import io.mango.calendar.api.query.CountWorkdaysQuery;
import io.mango.calendar.core.entity.Calendar;
import io.mango.calendar.core.entity.CalendarDay;
import io.mango.calendar.core.mapper.CalendarMapper;
import io.mango.calendar.core.service.ICalendarService;
import io.mango.calendar.core.support.CalendarDayTypes;
import io.mango.common.exception.BizException;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.infra.kv.api.ICache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration.class,
        CalendarServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:calendar;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false"
})
class CalendarServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CalendarMapper calendarMapper;

    @Autowired
    private ICalendarService calendarService;

    @Autowired
    private RecordingCache recordingCache;

    @BeforeEach
    void setUp() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        recordingCache.clear();
        jdbcTemplate.execute("DROP TABLE IF EXISTS calendar_day");
        jdbcTemplate.execute("DROP TABLE IF EXISTS calendar");
        jdbcTemplate.execute("""
                CREATE TABLE calendar (
                    id BIGINT NOT NULL,
                    tenant_id BIGINT NOT NULL DEFAULT 0,
                    calendar_code VARCHAR(64) NOT NULL,
                    calendar_name VARCHAR(128) NOT NULL,
                    status TINYINT NOT NULL DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_calendar_tenant_code (tenant_id, calendar_code)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE calendar_day (
                    id BIGINT NOT NULL,
                    tenant_id BIGINT NOT NULL DEFAULT 0,
                    calendar_id BIGINT NOT NULL,
                    calendar_year INT NOT NULL,
                    calendar_date DATE NOT NULL,
                    day_of_week TINYINT NOT NULL,
                    day_type VARCHAR(32) NOT NULL,
                    workday TINYINT NOT NULL,
                    day_name VARCHAR(128),
                    lunar_year INT,
                    lunar_month INT,
                    lunar_day INT,
                    lunar_leap_month TINYINT,
                    lunar_text VARCHAR(32),
                    ganzhi_year VARCHAR(16),
                    zodiac VARCHAR(16),
                    solar_term VARCHAR(16),
                    source VARCHAR(64),
                    remark VARCHAR(256),
                    enabled TINYINT NOT NULL DEFAULT 1,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_calendar_day_date (tenant_id, calendar_id, calendar_date)
                )
                """);
        seedCalendar();
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void isWorkday_weekday_returnsTrue() {
        assertThat(calendarService.isWorkday(dateQuery(LocalDate.of(2026, 2, 16)))).isTrue();
    }

    @Test
    void isWorkday_weekend_returnsFalse() {
        assertThat(calendarService.isWorkday(dateQuery(LocalDate.of(2026, 2, 14)))).isFalse();
    }

    @Test
    void isWorkday_adjustedWeekend_returnsTrue() {
        assertThat(calendarService.isWorkday(dateQuery(LocalDate.of(2026, 2, 15)))).isTrue();
    }

    @Test
    void isWorkday_specialHoliday_returnsFalse() {
        assertThat(calendarService.isWorkday(dateQuery(LocalDate.of(2026, 2, 17)))).isFalse();
    }

    @Test
    void nextWorkday_skipsHolidayAndWeekend() {
        assertThat(calendarService.nextWorkday(dateQuery(LocalDate.of(2026, 2, 16))))
                .isEqualTo(LocalDate.of(2026, 2, 18));
    }

    @Test
    void addWorkdays_crossYear_returnsExpectedDate() {
        AddWorkdaysQuery query = new AddWorkdaysQuery();
        query.setCalendarCode("CN_STANDARD");
        query.setSourceDate(LocalDate.of(2026, 12, 31));
        query.setAmount(2);

        assertThat(calendarService.addWorkdays(query)).isEqualTo(LocalDate.of(2027, 1, 5));
    }

    @Test
    void addWorkdays_negativeAmount_returnsPreviousWorkday() {
        AddWorkdaysQuery query = new AddWorkdaysQuery();
        query.setCalendarCode("CN_STANDARD");
        query.setSourceDate(LocalDate.of(2026, 2, 18));
        query.setAmount(-1);

        assertThat(calendarService.addWorkdays(query)).isEqualTo(LocalDate.of(2026, 2, 16));
    }

    @Test
    void addWorkdays_zeroAmount_returnsSourceDate() {
        AddWorkdaysQuery query = new AddWorkdaysQuery();
        query.setCalendarCode("CN_STANDARD");
        query.setSourceDate(LocalDate.of(2026, 2, 18));
        query.setAmount(0);

        assertThat(calendarService.addWorkdays(query)).isEqualTo(LocalDate.of(2026, 2, 18));
    }

    @Test
    void countWorkdays_includeBoundary_countsWorkdays() {
        CountWorkdaysQuery query = new CountWorkdaysQuery();
        query.setCalendarCode("CN_STANDARD");
        query.setStartDate(LocalDate.of(2026, 2, 15));
        query.setEndDate(LocalDate.of(2026, 2, 18));
        query.setIncludeStart(true);
        query.setIncludeEnd(true);

        assertThat(calendarService.countWorkdays(query)).isEqualTo(3);
    }

    @Test
    void countWorkdays_reversedRange_throwsException() {
        CountWorkdaysQuery query = new CountWorkdaysQuery();
        query.setCalendarCode("CN_STANDARD");
        query.setStartDate(LocalDate.of(2026, 2, 18));
        query.setEndDate(LocalDate.of(2026, 2, 16));

        assertThatThrownBy(() -> calendarService.countWorkdays(query))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("开始日期不能晚于结束日期");
    }

    @Test
    void batchCheck_returnsDayTypes() {
        BatchCheckWorkdayQuery query = new BatchCheckWorkdayQuery();
        query.setCalendarCode("CN_STANDARD");
        query.setDates(List.of(
                LocalDate.of(2026, 2, 15),
                LocalDate.of(2026, 2, 17),
                LocalDate.of(2026, 2, 18)
        ));

        assertThat(calendarService.batchCheck(query))
                .extracting("dayType")
                .containsExactly(CalendarDayType.ADJUSTED_WORKDAY, CalendarDayType.LEGAL_HOLIDAY, CalendarDayType.WORKDAY);
    }

    @Test
    void isWorkday_missingCalendar_throwsException() {
        CalendarDateQuery query = dateQuery(LocalDate.of(2026, 2, 16));
        query.setCalendarCode("MISSING");

        assertThatThrownBy(() -> calendarService.isWorkday(query))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("日历不存在或未启用");
    }

    @Test
    void isWorkday_sameDay_hitsKvCache() {
        CalendarDateQuery query = dateQuery(LocalDate.of(2026, 2, 17));

        assertThat(calendarService.isWorkday(query)).isFalse();
        jdbcTemplate.update("UPDATE calendar_day SET day_type = ?, workday = ? WHERE calendar_date = ?",
                CalendarDayType.WORKDAY.name(), 1, LocalDate.of(2026, 2, 17));

        assertThat(calendarService.isWorkday(query)).isFalse();
        assertThat(recordingCache.getCount()).isGreaterThanOrEqualTo(2);
        assertThat(recordingCache.setCount()).isEqualTo(1);
    }

    private void seedCalendar() {
        Calendar calendar = new Calendar();
        calendar.setId(1L);
        calendar.setTenantId(1L);
        calendar.setCalendarCode("CN_STANDARD");
        calendar.setCalendarName("中国标准工作日历");
        calendar.setStatus(1);
        calendarMapper.insert(calendar);

        seedYear(2026);
        seedYear(2027);
        updateDay(LocalDate.of(2026, 2, 15), CalendarDayType.ADJUSTED_WORKDAY, "春节调休上班");
        updateDay(LocalDate.of(2026, 2, 17), CalendarDayType.LEGAL_HOLIDAY, "春节");
        updateDay(LocalDate.of(2027, 1, 1), CalendarDayType.LEGAL_HOLIDAY, "元旦");
    }

    private void seedYear(int year) {
        LocalDate date = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        long id = year * 10000L;
        while (!date.isAfter(endDate)) {
            CalendarDayType dayType = CalendarDayTypes.defaultType(date.getDayOfWeek());
            jdbcTemplate.update("""
                    INSERT INTO calendar_day
                    (id, tenant_id, calendar_id, calendar_year, calendar_date, day_of_week, day_type, workday, enabled)
                    VALUES (?, 1, 1, ?, ?, ?, ?, ?, 1)
                    """,
                    id++,
                    year,
                    date,
                    date.getDayOfWeek().getValue(),
                    dayType.name(),
                    CalendarDayTypes.isWorkday(dayType) ? 1 : 0);
            date = date.plusDays(1);
        }
    }

    private void updateDay(LocalDate date, CalendarDayType dayType, String dayName) {
        jdbcTemplate.update("""
                UPDATE calendar_day
                SET day_type = ?, workday = ?, day_name = ?
                WHERE tenant_id = 1 AND calendar_id = 1 AND calendar_date = ?
                """,
                dayType.name(),
                CalendarDayTypes.isWorkday(dayType) ? 1 : 0,
                dayName,
                date);
    }

    private CalendarDateQuery dateQuery(LocalDate date) {
        CalendarDateQuery query = new CalendarDateQuery();
        query.setCalendarCode("CN_STANDARD");
        query.setDate(date);
        return query;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @MapperScan(basePackageClasses = CalendarMapper.class)
    @ComponentScan(basePackageClasses = CalendarServiceImpl.class)
    static class TestConfig {

        @Bean
        PlatformTransactionManager platformTransactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        RecordingCache recordingCache() {
            return new RecordingCache();
        }
    }

    static class RecordingCache implements ICache {

        private final Map<String, String> values = new HashMap<>();
        private int getCount;
        private int setCount;

        @Override
        public void set(String key, String value, long ttlSeconds) {
            setCount++;
            values.put(key, value);
        }

        @Override
        public String get(String key) {
            getCount++;
            return values.get(key);
        }

        @Override
        public boolean exists(String key) {
            return values.containsKey(key);
        }

        @Override
        public void delete(String key) {
            values.remove(key);
        }

        void clear() {
            values.clear();
            getCount = 0;
            setCount = 0;
        }

        int getCount() {
            return getCount;
        }

        int setCount() {
            return setCount;
        }
    }
}
