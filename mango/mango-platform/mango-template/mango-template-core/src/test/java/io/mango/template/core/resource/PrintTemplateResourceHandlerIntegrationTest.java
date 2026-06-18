package io.mango.template.core.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.template.core.mapper.TemplateCategoryMapper;
import io.mango.template.core.mapper.TemplateMapper;
import io.mango.template.core.mapper.TemplateRenderRecordMapper;
import io.mango.template.core.mapper.TemplateVersionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PrintTemplateResourceHandlerIntegrationTest {

    private AnnotationConfigApplicationContext context;
    private DataSource dataSource;
    private PrintTemplateResourceHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "spring.datasource.url", "jdbc:h2:mem:print_template_resource;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.username", "sa",
                "spring.datasource.password", "",
                "spring.datasource.driver-class-name", "org.h2.Driver",
                "spring.flyway.enabled", "false",
                "mango.persistence.mybatis-plus.tenant.enabled", "false"
        )));
        context.register(TestConfig.class);
        context.refresh();
        dataSource = context.getBean(DataSource.class);
        handler = context.getBean(PrintTemplateResourceHandler.class);
        rebuildTables();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void upsertCreatesTemplateCategoryTemplateAndVersion() throws Exception {
        handler.upsert(declaration());

        assertThat(count("template_category")).isOne();
        assertThat(count("template")).isOne();
        assertThat(count("template_version")).isOne();
        assertThat(stringValue("template_category", "category_name", "id = 3000100000000000002"))
                .isEqualTo("合同模板");
        assertThat(stringValue("template", "template_code", "id = 3000100000000000001"))
                .isEqualTo("contract.notice.default");
        assertThat(stringValue("template", "business_key", "id = 3000100000000000001"))
                .isEqualTo("contract.notice.default");
        assertThat(intValue("template", "current_version_no", "id = 3000100000000000001")).isOne();
        assertThat(stringValue("template_version", "content", "template_id = 3000100000000000001"))
                .isEqualTo("合同编号：{{contractNo}}");
        assertThat(intValue("template_version", "current_published", "template_id = 3000100000000000001"))
                .isOne();
    }

    @Test
    void upsertUpdatesTemplateAndSwitchesPublishedVersion() throws Exception {
        ResourceDeclaration declaration = declaration();
        handler.upsert(declaration);

        declaration.setVersion(2);
        declaration.getFields().get("templateName").setValue("合同通知模板V2");
        declaration.getFields().get("content").setValue("合同名称：{{contractName}}");
        declaration.getFields().get("versionId").setValue(3000100000000000004L);
        declaration.getFields().put("versionNo", field(ResourceFieldType.INT, 2));
        handler.upsert(declaration);

        assertThat(count("template")).isOne();
        assertThat(count("template_version")).isEqualTo(2);
        assertThat(stringValue("template", "template_name", "id = 3000100000000000001"))
                .isEqualTo("合同通知模板V2");
        assertThat(intValue("template", "current_version_no", "id = 3000100000000000001")).isEqualTo(2);
        assertThat(intValue("template_version", "current_published",
                "template_id = 3000100000000000001 and version_no = 1")).isZero();
        assertThat(intValue("template_version", "current_published",
                "template_id = 3000100000000000001 and version_no = 2")).isOne();
    }

    @Test
    void disableAndDeleteTemplate() throws Exception {
        ResourceDeclaration declaration = declaration();
        handler.upsert(declaration);
        execute("""
                insert into template_render_record
                (id, tenant_id, template_id, template_code, version_id, version_no, output_format, status)
                values (3000100000000000010, 1, 3000100000000000001, 'contract.notice.default',
                        3000100000000000003, 1, 'TEXT', 'SUCCESS')
                """);

        handler.disable(declaration);

        assertThat(intValue("template", "status", "id = 3000100000000000001")).isZero();

        handler.delete(declaration);

        assertThat(count("template")).isZero();
        assertThat(count("template_version")).isZero();
        assertThat(count("template_render_record")).isZero();
        assertThat(count("template_category")).isOne();
    }

    private ResourceDeclaration declaration() {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("3000100000000000001");
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.PRINT_TEMPLATE);
        declaration.setModuleCode("contract");
        declaration.setModuleName("合同");
        declaration.setBizKey("contract.notice.default");
        declaration.setName("合同通知模板");
        declaration.setTargetModule("template");
        declaration.setSource("PrintTemplateResourceHandlerIntegrationTest");
        declaration.setFields(new LinkedHashMap<>());
        declaration.getFields().put("templateId", field(ResourceFieldType.LONG, 3000100000000000001L));
        declaration.getFields().put("categoryId", field(ResourceFieldType.LONG, 3000100000000000002L));
        declaration.getFields().put("versionId", field(ResourceFieldType.LONG, 3000100000000000003L));
        declaration.getFields().put("tenantId", field(ResourceFieldType.LONG, 1L));
        declaration.getFields().put("templateCode", field(ResourceFieldType.STRING, "contract.notice.default"));
        declaration.getFields().put("templateName", field(ResourceFieldType.STRING, "合同通知模板"));
        declaration.getFields().put("categoryCode", field(ResourceFieldType.STRING, "CONTRACT"));
        declaration.getFields().put("categoryName", field(ResourceFieldType.STRING, "合同模板"));
        declaration.getFields().put("domainCode", field(ResourceFieldType.STRING, "CONTRACT"));
        declaration.getFields().put("sourceFormat", field(ResourceFieldType.STRING, "TEXT"));
        declaration.getFields().put("content", field(ResourceFieldType.STRING, "合同编号：{{contractNo}}"));
        declaration.getFields().put("variableSchema", field(ResourceFieldType.JSON, "[{\"name\":\"contractNo\"}]"));
        declaration.getFields().put("versionNo", field(ResourceFieldType.INT, 1));
        declaration.getFields().put("status", field(ResourceFieldType.INT, 1));
        return declaration;
    }

    private ResourceField field(ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        return field;
    }

    private void rebuildTables() throws Exception {
        execute("drop table if exists template_render_record");
        execute("drop table if exists template_version");
        execute("drop table if exists template");
        execute("drop table if exists template_category");
        execute("""
                create table template_category (
                    id bigint not null,
                    tenant_id bigint not null,
                    category_code varchar(64) not null,
                    category_name varchar(128) not null,
                    sort int not null default 0,
                    status tinyint not null default 1,
                    remark varchar(255),
                    created_by bigint,
                    created_time timestamp not null default current_timestamp,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_time timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id),
                    unique key uk_template_category_tenant_code (tenant_id, category_code)
                )
                """);
        execute("""
                create table template (
                    id bigint not null,
                    tenant_id bigint not null,
                    template_code varchar(128) not null,
                    template_name varchar(128) not null,
                    category_code varchar(64),
                    category_name varchar(64),
                    domain_code varchar(64),
                    business_group varchar(64),
                    business_type varchar(64),
                    business_key varchar(128),
                    source_format varchar(32),
                    status tinyint not null default 1,
                    current_version_no int not null default 0,
                    draft_source_format varchar(32),
                    draft_content longtext,
                    draft_source_file_id bigint,
                    draft_variable_schema json,
                    has_unpublished_changes tinyint not null default 0,
                    remark varchar(255),
                    created_by bigint,
                    created_time timestamp not null default current_timestamp,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_time timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id),
                    unique key uk_template_tenant_code (tenant_id, template_code),
                    unique key uk_template_tenant_business_key (tenant_id, business_key)
                )
                """);
        execute("""
                create table template_version (
                    id bigint not null,
                    tenant_id bigint not null,
                    template_id bigint not null,
                    version_no int not null,
                    source_format varchar(32) not null,
                    content longtext,
                    source_file_id bigint,
                    variable_schema json,
                    current_published tinyint not null default 0,
                    version_remark varchar(255),
                    created_by bigint,
                    created_time timestamp not null default current_timestamp,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_time timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id),
                    unique key uk_template_version_no (template_id, version_no)
                )
                """);
        execute("""
                create table template_render_record (
                    id bigint not null,
                    tenant_id bigint not null,
                    template_id bigint not null,
                    template_code varchar(128) not null,
                    version_id bigint not null,
                    version_no int not null,
                    output_format varchar(32) not null,
                    status varchar(32) not null,
                    output_file_id bigint,
                    output_content longtext,
                    error_message varchar(1000),
                    variable_payload json,
                    biz_type varchar(64),
                    biz_id varchar(128),
                    created_by bigint,
                    created_time timestamp not null default current_timestamp,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_time timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id)
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
    @Import({
            DataSourceAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            PersistenceMybatisPlusAutoConfiguration.class,
            PrintTemplateResourceHandler.class
    })
    @MapperScan(basePackageClasses = {
            TemplateMapper.class,
            TemplateCategoryMapper.class,
            TemplateVersionMapper.class,
            TemplateRenderRecordMapper.class
    })
    static class TestConfig {
    }
}
