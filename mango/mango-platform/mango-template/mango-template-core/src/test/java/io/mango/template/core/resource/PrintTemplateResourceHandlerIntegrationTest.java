package io.mango.template.core.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.fileproc.render.service.DefaultRenderApi;
import io.mango.infra.fileproc.render.service.FreemarkerRenderEngine;
import io.mango.infra.fileproc.render.service.RenderRegistry;
import io.mango.infra.fileproc.render.service.TextRenderProvider;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.template.api.command.CreateTemplateCommand;
import io.mango.template.api.command.PublishTemplateVersionCommand;
import io.mango.template.api.command.TemplateJsonRequest;
import io.mango.template.api.command.TemplateRenderCommand;
import io.mango.template.api.command.TemplateVariableCommand;
import io.mango.template.api.enums.TemplateOutputFormat;
import io.mango.template.api.enums.TemplateSourceFormat;
import io.mango.template.api.vo.TemplateRenderResultVO;
import io.mango.template.core.mapper.TemplateCategoryMapper;
import io.mango.template.core.mapper.TemplateMapper;
import io.mango.template.core.mapper.TemplateRenderRecordMapper;
import io.mango.template.core.mapper.TemplateVersionMapper;
import io.mango.template.core.render.TemplateRenderManager;
import io.mango.template.core.service.ITemplateFileStore;
import io.mango.template.core.service.TemplateDomainInfo;
import io.mango.template.core.service.TemplateStoredFile;
import io.mango.template.core.service.impl.TemplateServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
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
        MangoContextHolder.clear();
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
        declaration.putField("versionNo", field(ResourceFieldType.INT, 2));
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

    @Test
    void serviceCreatesPublishesAndRendersAgainstRealPersistenceAndRenderEngine() throws Exception {
        MangoContextHolder.set(MangoContextSnapshot.request("request-1", "trace-1", "1", "internal-admin", "127.0.0.1")
                .withSecurity(1001L, "1", "tester", "internal", "USER", "TENANT", 1L, "internal-admin"));
        DefaultRenderApi renderApi = new DefaultRenderApi(
                new RenderRegistry(List.of(new TextRenderProvider(new FreemarkerRenderEngine()))), null);
        TemplateServiceImpl service = new TemplateServiceImpl(
                context.getBean(TemplateMapper.class),
                context.getBean(TemplateVersionMapper.class),
                context.getBean(TemplateRenderRecordMapper.class),
                new TemplateRenderManager(renderApi, null),
                unsupportedFileStore(),
                new ObjectMapper().findAndRegisterModules(),
                Runnable::run,
                domainCode -> new TemplateDomainInfo(domainCode, "合同域", 1));

        CreateTemplateCommand create = new CreateTemplateCommand();
        create.setTemplateCode("contract.notice.integration");
        create.setTemplateName("合同通知集成模板");
        create.setDomainCode("CONTRACT");
        Long templateId = service.create(create);

        TemplateVariableCommand variable = new TemplateVariableCommand();
        variable.setName("contractNo");
        variable.setType("STRING");
        variable.setRequired(true);
        PublishTemplateVersionCommand publish = new PublishTemplateVersionCommand();
        publish.setTemplateId(templateId);
        publish.setSourceFormat(TemplateSourceFormat.TEXT);
        publish.setContent("合同编号：${contractNo}");
        publish.setVariables(List.of(variable));
        Long versionId = service.publishVersion(publish);

        TemplateRenderCommand render = new TemplateRenderCommand();
        render.setTemplateCode("contract.notice.integration");
        render.setOutputFormat(TemplateOutputFormat.TEXT);
        render.setVariables(TemplateJsonRequest.of(Map.of("contractNo", "M-2026-001")));
        TemplateRenderResultVO result = service.render(render);

        assertThat(templateId).isPositive();
        assertThat(versionId).isPositive();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getContent()).isEqualTo("合同编号：M-2026-001");
        assertThat(service.detail(templateId).getCurrentVersionNo()).isOne();
        assertThat(stringValue("template_render_record", "status", "template_id = " + templateId))
                .isEqualTo("SUCCESS");
        assertThat(stringValue("template_render_record", "output_content", "template_id = " + templateId))
                .isEqualTo("合同编号：M-2026-001");
    }

    private ITemplateFileStore unsupportedFileStore() {
        return new ITemplateFileStore() {
            @Override
            public Long save(byte[] content, String fileName, String contentType,
                             String purpose, String bizType, String bizId) {
                throw new UnsupportedOperationException("文本渲染不应保存文件");
            }

            @Override
            public TemplateStoredFile read(Long fileId) {
                throw new UnsupportedOperationException("文本渲染不应读取文件");
            }
        };
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
        declaration.putField("templateId", field(ResourceFieldType.LONG, 3000100000000000001L));
        declaration.putField("categoryId", field(ResourceFieldType.LONG, 3000100000000000002L));
        declaration.putField("versionId", field(ResourceFieldType.LONG, 3000100000000000003L));
        declaration.putField("tenantId", field(ResourceFieldType.LONG, 1L));
        declaration.putField("templateCode", field(ResourceFieldType.STRING, "contract.notice.default"));
        declaration.putField("templateName", field(ResourceFieldType.STRING, "合同通知模板"));
        declaration.putField("categoryCode", field(ResourceFieldType.STRING, "CONTRACT"));
        declaration.putField("categoryName", field(ResourceFieldType.STRING, "合同模板"));
        declaration.putField("domainCode", field(ResourceFieldType.STRING, "CONTRACT"));
        declaration.putField("sourceFormat", field(ResourceFieldType.STRING, "TEXT"));
        declaration.putField("content", field(ResourceFieldType.STRING, "合同编号：{{contractNo}}"));
        declaration.putField("variableSchema", field(ResourceFieldType.JSON, "[{\"name\":\"contractNo\"}]"));
        declaration.putField("versionNo", field(ResourceFieldType.INT, 1));
        declaration.putField("status", field(ResourceFieldType.INT, 1));
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
                    tenant_id varchar(64) not null,
                    org_id bigint,
                    category_code varchar(64) not null,
                    category_name varchar(128) not null,
                    sort int not null default 0,
                    status tinyint not null default 1,
                    remark varchar(255),
                    created_by bigint,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id),
                    unique key uk_template_category_tenant_code (tenant_id, category_code)
                )
                """);
        execute("""
                create table template (
                    id bigint not null,
                    tenant_id varchar(64) not null,
                    org_id bigint,
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
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id),
                    unique key uk_template_tenant_code (tenant_id, template_code),
                    unique key uk_template_tenant_business_key (tenant_id, business_key)
                )
                """);
        execute("""
                create table template_version (
                    id bigint not null,
                    tenant_id varchar(64) not null,
                    org_id bigint,
                    template_id bigint not null,
                    version_no int not null,
                    source_format varchar(32) not null,
                    content longtext,
                    source_file_id bigint,
                    variable_schema json,
                    current_published tinyint not null default 0,
                    version_remark varchar(255),
                    created_by bigint,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id),
                    unique key uk_template_version_no (template_id, version_no)
                )
                """);
        execute("""
                create table template_render_record (
                    id bigint not null,
                    tenant_id varchar(64) not null,
                    org_id bigint,
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
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
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
