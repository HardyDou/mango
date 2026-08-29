package io.mango.calendar.starter.resource;

import io.mango.calendar.core.mapper.CalendarDayMapper;
import io.mango.calendar.core.mapper.CalendarMapper;
import io.mango.calendar.core.service.CalendarAdminService;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.support.model.ResourceSyncResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CalendarResourceHandlerIntegrationTest.TestApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:calendar_resource_handler;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.autoconfigure.exclude=io.mango.calendar.starter.CalendarAutoConfiguration",
        "spring.flyway.enabled=false",
        "mango.persistence.flyway.enabled=false",
        "mango.persistence.schema-validation.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=true",
        "mango.persistence.mybatis-plus.tenant.default-tenant-id=1"
})
class CalendarResourceHandlerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CalendarDefinitionResourceHandler definitionHandler;

    @Autowired
    private CalendarYearResourceHandler yearHandler;

    @BeforeEach
    void setUp() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("ambient"));
        recreateSchema();
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void portableResources_twoEmptyDatabases_produceIdenticalCalendarIds() {
        applyPortableCalendar();
        Long firstCalendarId = calendarId();
        List<Long> firstDayIds = dayIds();

        recreateSchema();
        applyPortableCalendar();

        assertThat(calendarId()).isEqualTo(firstCalendarId);
        assertThat(dayIds()).containsExactlyElementsOf(firstDayIds).hasSize(365);
        assertThat(calendarIdsOnDays()).containsOnly(firstCalendarId);
        assertThat(MangoContextHolder.tenantId()).isEqualTo("ambient");
    }

    @Test
    void definition_naturalKeyAlreadyExists_preservesExistingId() {
        jdbcTemplate.update("""
                insert into calendar (id, tenant_id, calendar_code, calendar_name, status)
                values (77, '1', 'CN_STANDARD', 'existing', 0)
                """);

        ResourceSyncResult result = definitionHandler.upsert(definition());

        assertThat(result.getTargetId()).isEqualTo(77L);
        assertThat(calendarId()).isEqualTo(77L);
        assertThat(jdbcTemplate.queryForObject(
                "select calendar_name from calendar where id = 77", String.class))
                .isEqualTo("中国标准工作日历");
        assertThat(MangoContextHolder.tenantId()).isEqualTo("ambient");
    }

    private void applyPortableCalendar() {
        definitionHandler.upsert(definition());
        yearHandler.upsert(year());
    }

    private ResourceDeclaration definition() {
        ResourceDeclaration declaration = declaration("it-calendar-definition", CalendarResourceTypes.DEFINITION);
        put(declaration, "tenantId", ResourceFieldType.STRING, "1");
        put(declaration, "calendarCode", ResourceFieldType.STRING, "CN_STANDARD");
        put(declaration, "calendarName", ResourceFieldType.STRING, "中国标准工作日历");
        put(declaration, "status", ResourceFieldType.INT, 1);
        return declaration;
    }

    private ResourceDeclaration year() {
        ResourceDeclaration declaration = declaration("it-calendar-year-2026", CalendarResourceTypes.YEAR);
        put(declaration, "tenantId", ResourceFieldType.STRING, "1");
        put(declaration, "calendarCode", ResourceFieldType.STRING, "CN_STANDARD");
        put(declaration, "year", ResourceFieldType.INT, 2026);
        put(declaration, "items", ResourceFieldType.LIST, List.of());
        return declaration;
    }

    private ResourceDeclaration declaration(String id, String resourceType) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(id);
        declaration.setVersion(1);
        declaration.setResourceType(resourceType);
        declaration.setModuleCode("calendar");
        declaration.setBizKey("calendar." + id);
        declaration.setName(id);
        declaration.setTargetModule("calendar");
        declaration.setFields(new LinkedHashMap<>());
        return declaration;
    }

    private void put(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.putField(name, field);
    }

    private Long calendarId() {
        return jdbcTemplate.queryForObject(
                "select id from calendar where tenant_id = '1' and calendar_code = 'CN_STANDARD'", Long.class);
    }

    private List<Long> dayIds() {
        return jdbcTemplate.queryForList(
                "select id from calendar_day where tenant_id = '1' order by calendar_date", Long.class);
    }

    private List<Long> calendarIdsOnDays() {
        return jdbcTemplate.queryForList(
                "select distinct calendar_id from calendar_day where tenant_id = '1'", Long.class);
    }

    private void recreateSchema() {
        jdbcTemplate.execute("drop table if exists calendar_day");
        jdbcTemplate.execute("drop table if exists calendar");
        jdbcTemplate.execute("""
                create table calendar (
                    id bigint not null primary key,
                    tenant_id varchar(64) not null,
                    org_id bigint,
                    calendar_code varchar(64) not null,
                    calendar_name varchar(128) not null,
                    status tinyint not null default 1,
                    created_by bigint,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp not null default current_timestamp,
                    constraint uk_calendar_tenant_code unique (tenant_id, calendar_code)
                )
                """);
        jdbcTemplate.execute("""
                create table calendar_day (
                    id bigint not null primary key,
                    tenant_id varchar(64) not null,
                    org_id bigint,
                    calendar_id bigint not null,
                    calendar_year int not null,
                    calendar_date date not null,
                    day_of_week tinyint not null,
                    day_type varchar(32) not null,
                    workday tinyint not null,
                    day_name varchar(128),
                    lunar_year int,
                    lunar_month int,
                    lunar_day int,
                    lunar_leap_month tinyint default 0,
                    lunar_text varchar(32),
                    ganzhi_year varchar(16),
                    zodiac varchar(16),
                    solar_term varchar(16),
                    source varchar(64),
                    remark varchar(256),
                    enabled tinyint not null default 1,
                    created_by bigint,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp not null default current_timestamp,
                    constraint uk_calendar_day_date unique (tenant_id, calendar_id, calendar_date)
                )
                """);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackageClasses = {CalendarMapper.class, CalendarDayMapper.class})
    @ComponentScan(basePackageClasses = CalendarAdminService.class)
    @Import({CalendarDefinitionResourceHandler.class, CalendarYearResourceHandler.class})
    static class TestApplication {
    }
}
