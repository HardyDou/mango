package io.mango.system.core.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.system.core.mapper.SysConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        SystemConfigResourceHandlerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:system_config_resource;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class SystemConfigResourceHandlerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SystemConfigResourceHandler handler;

    @BeforeEach
    void setUp() {
        rebuildTables();
    }

    @Test
    void upsertCreatesConfig() {
        handler.upsert(configDeclaration("skin-blue", 1));

        assertThat(stringValue("sys_config", "config_value", "id = 1")).isEqualTo("skin-blue");
        assertThat(stringValue("sys_config", "type", "id = 1")).isEqualTo("SYSTEM");
        assertThat(stringValue("sys_config", "domain_code", "id = 1")).isEqualTo("COMMON");
    }

    @Test
    void upsertUpdatesConfig() {
        handler.upsert(configDeclaration("skin-blue", 1));

        handler.upsert(configDeclaration("skin-green", 2));

        assertThat(stringValue("sys_config", "config_value", "id = 1")).isEqualTo("skin-green");
        assertThat(intValue("sys_config", "sort", "id = 1")).isEqualTo(2);
    }

    @Test
    void disableMarksConfigDisabled() {
        ResourceDeclaration declaration = configDeclaration("skin-blue", 1);
        handler.upsert(declaration);

        handler.disable(declaration);

        assertThat(intValue("sys_config", "status", "id = 1")).isZero();
    }

    @Test
    void deletePhysicallyDeletesConfig() {
        ResourceDeclaration declaration = configDeclaration("skin-blue", 1);
        handler.upsert(declaration);

        handler.delete(declaration);

        assertThat(count("sys_config")).isZero();
    }

    private ResourceDeclaration configDeclaration(String value, int sort) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("2026061800500100001");
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.SYSTEM_CONFIG);
        declaration.setModuleCode("system");
        declaration.setBizKey("system.config.index-skin-name");
        declaration.setName("皮肤名称");
        declaration.setTargetModule("system");
        declaration.setFields(new LinkedHashMap<>());
        field(declaration, "configId", ResourceFieldType.LONG, 1L);
        field(declaration, "configKey", ResourceFieldType.STRING, "sys.index.skinName");
        field(declaration, "configValue", ResourceFieldType.STRING, value);
        field(declaration, "configName", ResourceFieldType.STRING, "皮肤名称");
        field(declaration, "type", ResourceFieldType.STRING, "SYSTEM");
        field(declaration, "domainCode", ResourceFieldType.STRING, "COMMON");
        field(declaration, "sort", ResourceFieldType.INT, sort);
        field(declaration, "status", ResourceFieldType.INT, 1);
        return declaration;
    }

    private void field(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.getFields().put(name, field);
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists sys_config");
        jdbcTemplate.execute("""
                create table sys_config (
                    id bigint not null,
                    config_key varchar(100) not null,
                    config_value clob not null,
                    config_name varchar(100) not null,
                    type varchar(20) not null,
                    domain_code varchar(64) not null default 'COMMON',
                    sort int not null default 0,
                    status tinyint not null default 1,
                    remark varchar(500),
                    create_by varchar(64),
                    update_by varchar(64),
                    create_time timestamp not null default current_timestamp,
                    update_time timestamp not null default current_timestamp,
                    primary key (id),
                    unique key config_key (config_key)
                )
                """);
    }

    private long count(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    private String stringValue(String tableName, String columnName, String whereClause) {
        return jdbcTemplate.queryForObject("select " + columnName + " from " + tableName + " where " + whereClause,
                String.class);
    }

    private int intValue(String tableName, String columnName, String whereClause) {
        Integer value = jdbcTemplate.queryForObject("select " + columnName + " from " + tableName + " where " + whereClause,
                Integer.class);
        return value == null ? 0 : value;
    }

    @Configuration
    @Import(SystemConfigResourceHandler.class)
    @MapperScan(basePackageClasses = SysConfigMapper.class)
    static class TestConfig {
    }
}
