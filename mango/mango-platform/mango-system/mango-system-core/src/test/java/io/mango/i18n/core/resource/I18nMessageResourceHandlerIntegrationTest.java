package io.mango.i18n.core.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.i18n.core.mapper.SysI18nMapper;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        I18nMessageResourceHandlerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:i18n_message_resource;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class I18nMessageResourceHandlerIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private I18nMessageResourceHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        rebuildTables();
    }

    @Test
    void upsertCreatesI18nMessage() throws Exception {
        handler.upsert(i18nDeclaration("common.submit", "提交", "Submit", "Submit button"));

        assertThat(stringValue("name", "id = 2026061900900010008")).isEqualTo("common.submit");
        assertThat(stringValue("zh_cn", "name = 'common.submit'")).isEqualTo("提交");
        assertThat(stringValue("en", "name = 'common.submit'")).isEqualTo("Submit");
    }

    @Test
    void upsertUpdatesExistingI18nMessageByName() throws Exception {
        handler.upsert(i18nDeclaration("common.submit", "提交", "Submit", "Submit button"));

        handler.upsert(i18nDeclaration("common.submit", "提交表单", "Submit Form", "Submit form button"));

        assertThat(count()).isEqualTo(1);
        assertThat(stringValue("zh_cn", "name = 'common.submit'")).isEqualTo("提交表单");
        assertThat(stringValue("description", "name = 'common.submit'")).isEqualTo("Submit form button");
    }

    @Test
    void deletePhysicallyDeletesI18nMessage() throws Exception {
        ResourceDeclaration declaration = i18nDeclaration("common.submit", "提交", "Submit", "Submit button");
        handler.upsert(declaration);

        handler.delete(declaration);

        assertThat(count()).isZero();
    }

    private ResourceDeclaration i18nDeclaration(String name, String zhCn, String en, String description) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("2026061900900010008");
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.I18N_MESSAGE);
        declaration.setModuleCode("system");
        declaration.setBizKey("system.i18n.common.submit");
        declaration.setName(name);
        declaration.setTargetModule("system");
        declaration.setFields(new LinkedHashMap<>());
        field(declaration, "i18nId", ResourceFieldType.LONG, 2026061900900010008L);
        field(declaration, "name", ResourceFieldType.STRING, name);
        field(declaration, "zhCn", ResourceFieldType.STRING, zhCn);
        field(declaration, "en", ResourceFieldType.STRING, en);
        field(declaration, "description", ResourceFieldType.STRING, description);
        return declaration;
    }

    private void field(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.getFields().put(name, field);
    }

    private void rebuildTables() throws Exception {
        execute("drop table if exists sys_i18n");
        execute("""
                create table sys_i18n (
                    id bigint primary key,
                    name varchar(255) not null,
                    zh_cn varchar(1000) not null,
                    en varchar(1000) not null,
                    description varchar(1000),
                    unique key uk_sys_i18n_name (name)
                )
                """);
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long count() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select count(*) from sys_i18n")) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private String stringValue(String columnName, String whereClause) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select " + columnName + " from sys_i18n where " + whereClause)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    @Configuration
    @Import(I18nMessageResourceHandler.class)
    @MapperScan(basePackageClasses = SysI18nMapper.class)
    static class TestConfig {
    }
}
