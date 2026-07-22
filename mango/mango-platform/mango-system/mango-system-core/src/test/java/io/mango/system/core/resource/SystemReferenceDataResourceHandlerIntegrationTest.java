package io.mango.system.core.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.area.core.mapper.SysAreaMapper;
import io.mango.area.core.resource.SystemAreaResourceHandler;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.system.core.mapper.SysTenantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        SystemReferenceDataResourceHandlerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:system_reference_data;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class SystemReferenceDataResourceHandlerIntegrationTest {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private SystemTenantResourceHandler tenantHandler;
    @Autowired
    private SystemAreaResourceHandler areaHandler;

    @BeforeEach
    void setUp() throws Exception {
        execute("drop table if exists sys_area");
        execute("drop table if exists sys_tenant");
        execute("""
                create table sys_tenant (
                    id bigint primary key,
                    tenant_name varchar(100) not null,
                    tenant_code varchar(50) not null,
                    institution_type varchar(32) not null,
                    capability_codes varchar(500),
                    package_id bigint,
                    status tinyint not null,
                    contact varchar(64),
                    mobile varchar(20),
                    email varchar(100),
                    remark varchar(500),
                    tenant_id varchar(64) not null,
                    org_id bigint,
                    created_by bigint,
                    created_at timestamp not null,
                    updated_by bigint,
                    updated_at timestamp not null,
                    unique key uk_sys_tenant_code (tenant_code)
                )
                """);
        execute("""
                create table sys_area (
                    id bigint primary key,
                    pid bigint not null,
                    name varchar(100) not null,
                    letter varchar(32),
                    adcode bigint,
                    location varchar(100),
                    area_sort int not null,
                    area_status varchar(8) not null,
                    area_type varchar(8) not null,
                    hot varchar(8) not null,
                    city_code varchar(32),
                    tenant_id varchar(64) not null,
                    org_id bigint,
                    created_by bigint,
                    created_at timestamp not null,
                    updated_by bigint,
                    updated_at timestamp not null
                )
                """);
    }

    @Test
    void tenantUpsertCreatesAndUpdatesRequiredData() throws Exception {
        ResourceDeclaration declaration = tenant("芒果集团");
        tenantHandler.upsert(declaration);
        field(declaration, "tenantName", ResourceFieldType.STRING, "芒果集团新版");
        tenantHandler.upsert(declaration);

        assertThat(value("sys_tenant", "tenant_name", "id = 1")).isEqualTo("芒果集团新版");
        assertThat(value("sys_tenant", "tenant_id", "id = 1")).isEqualTo("1");
        assertThat(count("sys_tenant")).isEqualTo(1);
    }

    @Test
    void tenantDeleteUsesSafeDisableSemantics() throws Exception {
        ResourceDeclaration declaration = tenant("芒果集团");
        tenantHandler.upsert(declaration);

        tenantHandler.delete(declaration);

        assertThat(value("sys_tenant", "status", "id = 1")).isEqualTo("0");
    }

    @Test
    void areaUpsertCreatesAndUpdatesRequiredData() throws Exception {
        ResourceDeclaration declaration = area("北京市");
        areaHandler.upsert(declaration);
        field(declaration, "name", ResourceFieldType.STRING, "北京市新版");
        areaHandler.upsert(declaration);

        assertThat(value("sys_area", "name", "id = 1")).isEqualTo("北京市新版");
        assertThat(value("sys_area", "tenant_id", "id = 1")).isEqualTo("1");
        assertThat(count("sys_area")).isEqualTo(1);
    }

    @Test
    void areaDeleteUsesSafeDisableSemantics() throws Exception {
        ResourceDeclaration declaration = area("北京市");
        areaHandler.upsert(declaration);

        areaHandler.delete(declaration);

        assertThat(value("sys_area", "area_status", "id = 1")).isEqualTo("0");
    }

    @Test
    void areaBatchUpsertPreservesInputOrderAndConvergesDuplicateTargetIds() throws Exception {
        areaHandler.upsert(area("2026071610000000001", 1L, "北京市旧值"));

        var results = areaHandler.upsertBatch(List.of(
                area("2026071610000000001", 1L, "北京市新版"),
                area("2026071610000000002", 2L, "天津市初值"),
                area("2026071610000000003", 2L, "天津市最终值")));

        assertThat(value("sys_area", "name", "id = 1")).isEqualTo("北京市新版");
        assertThat(value("sys_area", "name", "id = 2")).isEqualTo("天津市最终值");
        assertThat(count("sys_area")).isEqualTo(2);
        assertThat(results.get("2026071610000000001").getTargetId()).isEqualTo(1L);
        assertThat(results.get("2026071610000000003").getTargetId()).isEqualTo(2L);
    }

    private ResourceDeclaration tenant(String name) {
        ResourceDeclaration declaration = declaration("2026071609000000001", ResourceTypes.SYSTEM_TENANT);
        field(declaration, "targetId", ResourceFieldType.LONG, 1L);
        field(declaration, "tenantName", ResourceFieldType.STRING, name);
        field(declaration, "tenantCode", ResourceFieldType.STRING, "default");
        field(declaration, "institutionType", ResourceFieldType.STRING, "PLATFORM");
        field(declaration, "status", ResourceFieldType.INT, 1);
        return declaration;
    }

    private ResourceDeclaration area(String name) {
        return area("2026071610000000001", 1L, name);
    }

    private ResourceDeclaration area(String resourceId, Long targetId, String name) {
        ResourceDeclaration declaration = declaration(resourceId, ResourceTypes.SYSTEM_AREA);
        declaration.setBizKey("system.test.area." + resourceId);
        field(declaration, "targetId", ResourceFieldType.LONG, targetId);
        field(declaration, "pid", ResourceFieldType.LONG, 0L);
        field(declaration, "name", ResourceFieldType.STRING, name);
        field(declaration, "adcode", ResourceFieldType.LONG, 110000L);
        field(declaration, "areaSort", ResourceFieldType.INT, 1);
        field(declaration, "areaStatus", ResourceFieldType.STRING, "1");
        field(declaration, "areaType", ResourceFieldType.STRING, "1");
        field(declaration, "hot", ResourceFieldType.STRING, "1");
        return declaration;
    }

    private ResourceDeclaration declaration(String id, String type) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(id);
        declaration.setVersion(1);
        declaration.setResourceType(type);
        declaration.setModuleCode("system");
        declaration.setBizKey("system.test." + type.toLowerCase());
        declaration.setName(type);
        declaration.setTargetModule("system");
        declaration.setFields(new LinkedHashMap<>());
        return declaration;
    }

    private void field(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.putField(name, field);
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long count(String table) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select count(*) from " + table)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private String value(String table, String column, String whereClause) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select " + column + " from " + table + " where " + whereClause)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    @Configuration
    @Import({SystemTenantResourceHandler.class, SystemAreaResourceHandler.class})
    @MapperScan(basePackageClasses = {SysTenantMapper.class, SysAreaMapper.class})
    static class TestConfig {
    }
}
