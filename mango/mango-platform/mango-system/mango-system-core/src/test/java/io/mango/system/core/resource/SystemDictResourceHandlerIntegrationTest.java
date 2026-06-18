package io.mango.system.core.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.system.core.mapper.DictDataMapper;
import io.mango.system.core.mapper.DictTypeMapper;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        SystemDictResourceHandlerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:system_dict_resource;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class SystemDictResourceHandlerIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SystemDictResourceHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        rebuildTables();
    }

    @Test
    void upsertCreatesSystemDictAndItems() throws Exception {
        handler.upsert(dictDeclaration("授权角色类型", List.of(
                item(100L, "系统角色", "1", 1, 1),
                item(101L, "业务角色", "2", 2, 1)
        )));

        assertThat(stringValue("sys_dict_type", "dict_name", "dict_type = 'authorization_role_type'"))
                .isEqualTo("授权角色类型");
        assertThat(stringValue("sys_dict_type", "domain_code", "dict_type = 'authorization_role_type'"))
                .isEqualTo("AUTHORIZATION");
        assertThat(stringValue("sys_dict_data", "dict_label", "id = 100")).isEqualTo("系统角色");
        assertThat(count("sys_dict_data")).isEqualTo(2);
    }

    @Test
    void upsertUpdatesDictAndDisablesRemovedItems() throws Exception {
        handler.upsert(dictDeclaration("授权角色类型", List.of(
                item(100L, "系统角色", "1", 1, 1),
                item(101L, "业务角色", "2", 2, 1)
        )));

        handler.upsert(dictDeclaration("授权角色类型新版", List.of(
                item(100L, "系统内置角色", "1", 1, 1)
        )));

        assertThat(stringValue("sys_dict_type", "dict_name", "id = 10")).isEqualTo("授权角色类型新版");
        assertThat(stringValue("sys_dict_data", "dict_label", "id = 100")).isEqualTo("系统内置角色");
        assertThat(intValue("sys_dict_data", "status", "id = 101")).isZero();
    }

    @Test
    void disableMarksDictAndItemsDisabled() throws Exception {
        ResourceDeclaration declaration = dictDeclaration("授权角色类型", List.of(
                item(100L, "系统角色", "1", 1, 1)
        ));
        handler.upsert(declaration);

        handler.disable(declaration);

        assertThat(intValue("sys_dict_type", "status", "id = 10")).isZero();
        assertThat(intValue("sys_dict_data", "status", "id = 100")).isZero();
    }

    @Test
    void deletePhysicallyDeletesDictAndItems() throws Exception {
        ResourceDeclaration declaration = dictDeclaration("授权角色类型", List.of(
                item(100L, "系统角色", "1", 1, 1)
        ));
        handler.upsert(declaration);

        handler.delete(declaration);

        assertThat(count("sys_dict_type")).isZero();
        assertThat(count("sys_dict_data")).isZero();
    }

    private ResourceDeclaration dictDeclaration(String dictName, List<Map<String, Object>> items) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("2026061800100000001");
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.SYSTEM_DICT);
        declaration.setModuleCode("authorization");
        declaration.setBizKey("authorization.dict.role-type");
        declaration.setName("授权角色类型字典");
        declaration.setTargetModule("system");
        declaration.setFields(new LinkedHashMap<>());
        field(declaration, "typeId", ResourceFieldType.LONG, 10L);
        field(declaration, "dictType", ResourceFieldType.STRING, "authorization_role_type");
        field(declaration, "dictName", ResourceFieldType.STRING, dictName);
        field(declaration, "domainCode", ResourceFieldType.STRING, "AUTHORIZATION");
        field(declaration, "remark", ResourceFieldType.STRING, "授权模块角色类型");
        field(declaration, "items", ResourceFieldType.LIST, items);
        return declaration;
    }

    private void field(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.getFields().put(name, field);
    }

    private Map<String, Object> item(Long id, String label, String value, int sort, int status) {
        return Map.of(
                "id", id,
                "label", label,
                "value", value,
                "sort", sort,
                "status", status
        );
    }

    private void rebuildTables() throws Exception {
        execute("drop table if exists sys_dict_data");
        execute("drop table if exists sys_dict_type");
        execute("""
                create table sys_dict_type (
                    id bigint primary key,
                    dict_type varchar(50) not null,
                    dict_name varchar(100) not null,
                    domain_code varchar(64) not null default 'COMMON',
                    status tinyint not null default 1,
                    remark varchar(500),
                    create_by varchar(64),
                    update_by varchar(64),
                    create_time datetime not null default current_timestamp,
                    update_time datetime not null default current_timestamp,
                    unique key uk_sys_dict_type (dict_type)
                )
                """);
        execute("""
                create table sys_dict_data (
                    id bigint primary key,
                    dict_type varchar(50) not null,
                    dict_label varchar(100) not null,
                    dict_value varchar(100) not null,
                    sort int not null default 0,
                    status tinyint not null default 1,
                    remark varchar(500),
                    create_by varchar(64),
                    update_by varchar(64),
                    create_time datetime not null default current_timestamp,
                    update_time datetime not null default current_timestamp
                )
                """);
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long count(String tableName) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select count(*) from " + tableName)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private String stringValue(String tableName, String columnName, String whereClause) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select " + columnName + " from " + tableName + " where " + whereClause)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private int intValue(String tableName, String columnName, String whereClause) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select " + columnName + " from " + tableName + " where " + whereClause)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    @Configuration
    @Import(SystemDictResourceHandler.class)
    @MapperScan(basePackageClasses = {
            DictTypeMapper.class,
            DictDataMapper.class
    })
    static class TestConfig {
    }
}
