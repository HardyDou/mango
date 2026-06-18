package io.mango.notice.core.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.notice.core.mapper.NoticeBusinessChannelTemplateMapper;
import io.mango.notice.core.mapper.NoticeBusinessConfigVersionMapper;
import io.mango.notice.core.mapper.NoticeBusinessTypeMapper;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
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

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        NoticeMessageTemplateResourceHandlerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:notice_message_template_resource;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class NoticeMessageTemplateResourceHandlerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NoticeMessageTemplateResourceHandler handler;

    @BeforeEach
    void setUp() {
        rebuildTables();
    }

    @Test
    void upsertCreatesMessageTemplatePackage() {
        handler.upsert(messageTemplateDeclaration("定时任务执行失败：{{jobName}}", true));

        assertThat(count("notice_business_type")).isOne();
        assertThat(count("notice_business_config_version")).isOne();
        assertThat(count("notice_business_channel_template")).isOne();
        assertThat(stringValue("notice_business_type", "biz_type", "id = 2060000000000014001"))
                .isEqualTo("job.instance.failed");
        assertThat(stringValue("notice_business_type", "default_priority", "id = 2060000000000014001"))
                .isEqualTo("HIGH");
        assertThat(stringValue("notice_business_config_version", "version_status", "id = 2060000000000014002"))
                .isEqualTo("ACTIVE");
        assertThat(stringValue("notice_business_channel_template", "channel_type", "id = 2060000000000014003"))
                .isEqualTo("SITE");
    }

    @Test
    void upsertUpdatesMessageTemplatePackage() {
        handler.upsert(messageTemplateDeclaration("定时任务执行失败：{{jobName}}", true));

        handler.upsert(messageTemplateDeclaration("任务失败：{{jobName}}", true));

        assertThat(stringValue("notice_business_channel_template", "title_template", "id = 2060000000000014003"))
                .isEqualTo("任务失败：{{jobName}}");
        assertThat(count("notice_business_channel_template")).isOne();
    }

    @Test
    void disableMarksBusinessAndTemplateDisabled() {
        ResourceDeclaration declaration = messageTemplateDeclaration("定时任务执行失败：{{jobName}}", true);
        handler.upsert(declaration);

        handler.disable(declaration);

        assertThat(booleanValue("notice_business_type", "enabled", "id = 2060000000000014001")).isFalse();
        assertThat(booleanValue("notice_business_channel_template", "enabled", "id = 2060000000000014003")).isFalse();
    }

    @Test
    void deletePhysicallyDeletesMessageTemplatePackage() {
        ResourceDeclaration declaration = messageTemplateDeclaration("定时任务执行失败：{{jobName}}", true);
        handler.upsert(declaration);

        handler.delete(declaration);

        assertThat(count("notice_business_channel_template")).isZero();
        assertThat(count("notice_business_config_version")).isZero();
        assertThat(count("notice_business_type")).isZero();
    }

    @Test
    void deleteOneChannelKeepsSharedBusinessType() {
        ResourceDeclaration site = messageTemplateDeclaration("定时任务执行失败：{{jobName}}", true);
        ResourceDeclaration email = messageTemplateDeclaration("定时任务执行失败邮件：{{jobName}}", true);
        email.setBizKey("job.message.job-instance-failed-email");
        email.setName("定时任务执行失败邮件");
        field(email, "channelTemplateId", ResourceFieldType.LONG, 2060000000000014004L);
        field(email, "channelType", ResourceFieldType.STRING, "EMAIL");
        field(email, "templateName", ResourceFieldType.STRING, "定时任务执行失败邮件");
        handler.upsert(site);
        handler.upsert(email);

        handler.delete(email);

        assertThat(count("notice_business_type")).isOne();
        assertThat(count("notice_business_config_version")).isOne();
        assertThat(count("notice_business_channel_template")).isOne();
        assertThat(stringValue("notice_business_channel_template", "channel_type", "id = 2060000000000014003"))
                .isEqualTo("SITE");
    }

    @Test
    void jobYamlMessageTemplateMatchesOldFlywaySeed() throws Exception {
        List<ResourceDeclaration> declarations = loadJobMessageTemplateDeclarations();

        for (ResourceDeclaration declaration : declarations) {
            handler.upsert(declaration);
        }

        assertThat(count("notice_business_type")).isOne();
        assertThat(count("notice_business_config_version")).isOne();
        assertThat(count("notice_business_channel_template")).isOne();
        assertThat(stringValue("notice_business_type", "biz_type", "id = 2060000000000014001"))
                .isEqualTo("job.instance.failed");
        assertThat(stringValue("notice_business_type", "biz_name", "id = 2060000000000014001"))
                .isEqualTo("定时任务执行失败");
        assertThat(stringValue("notice_business_type", "tenant_id", "id = 2060000000000014001")).isEqualTo("1");
        assertThat(stringValue("notice_business_type", "params_schema", "id = 2060000000000014001"))
                .contains("\"required\":[\"jobCode\",\"jobName\",\"instanceId\",\"errorSummary\"]");
        assertThat(intValue("notice_business_config_version", "version", "id = 2060000000000014002")).isEqualTo(1);
        assertThat(stringValue("notice_business_config_version", "version_status", "id = 2060000000000014002"))
                .isEqualTo("ACTIVE");
        assertThat(stringValue("notice_business_channel_template", "title_template", "id = 2060000000000014003"))
                .isEqualTo("定时任务执行失败：{{jobName}}");
        assertThat(stringValue("notice_business_channel_template", "content_template", "id = 2060000000000014003"))
                .contains("请进入平台能力/任务管理/执行实例查看日志");
    }

    @SuppressWarnings("unchecked")
    private List<ResourceDeclaration> loadJobMessageTemplateDeclarations() throws Exception {
        Path path = Path.of("../../mango-job/mango-job-starter/src/main/resources/META-INF/mango/resources/job-common-message.yml");
        ObjectMapper objectMapper = new ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
        MapNode root = objectMapper.readValue(path.toFile(), MapNode.class);
        return ((List<ResourceDeclaration>) root.mango().resource().declarations().get(ResourceTypes.MESSAGE_TEMPLATE));
    }

    private ResourceDeclaration messageTemplateDeclaration(String titleTemplate, boolean enabled) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("2026061800700014001");
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.MESSAGE_TEMPLATE);
        declaration.setModuleCode("job");
        declaration.setBizKey("job.message.job-instance-failed-site");
        declaration.setName("定时任务执行失败系统消息");
        declaration.setTargetModule("notice");
        declaration.setFields(new LinkedHashMap<>());
        field(declaration, "businessTypeId", ResourceFieldType.LONG, 2060000000000014001L);
        field(declaration, "configVersionId", ResourceFieldType.LONG, 2060000000000014002L);
        field(declaration, "channelTemplateId", ResourceFieldType.LONG, 2060000000000014003L);
        field(declaration, "tenantId", ResourceFieldType.STRING, "1");
        field(declaration, "bizType", ResourceFieldType.STRING, "job.instance.failed");
        field(declaration, "bizName", ResourceFieldType.STRING, "定时任务执行失败");
        field(declaration, "bizGroup", ResourceFieldType.STRING, "JOB");
        field(declaration, "domainCode", ResourceFieldType.STRING, "JOB");
        field(declaration, "description", ResourceFieldType.STRING, "定时任务实例执行失败后发送给任务负责人或配置接收人。");
        field(declaration, "paramsSchema", ResourceFieldType.JSON, "{\"type\":\"object\"}");
        field(declaration, "enabled", ResourceFieldType.BOOLEAN, enabled);
        field(declaration, "defaultPriority", ResourceFieldType.STRING, "HIGH");
        field(declaration, "idempotentStrategy", ResourceFieldType.STRING, "BIZ_ID");
        field(declaration, "version", ResourceFieldType.INT, 1);
        field(declaration, "versionStatus", ResourceFieldType.STRING, "ACTIVE");
        field(declaration, "channelType", ResourceFieldType.STRING, "SITE");
        field(declaration, "templateName", ResourceFieldType.STRING, "定时任务执行失败系统消息");
        field(declaration, "titleTemplate", ResourceFieldType.STRING, titleTemplate);
        field(declaration, "contentTemplate", ResourceFieldType.STRING,
                "定时任务 {{jobName}}（{{jobCode}}）执行失败。实例：{{instanceId}}；处理器：{{handlerName}}；触发批次：{{triggerBatchNo}}；失败原因：{{errorSummary}}。请进入平台能力/任务管理/执行实例查看日志。");
        field(declaration, "operatorId", ResourceFieldType.LONG, 1L);
        return declaration;
    }

    private void field(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.getFields().put(name, field);
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists notice_business_channel_template");
        jdbcTemplate.execute("drop table if exists notice_business_config_version");
        jdbcTemplate.execute("drop table if exists notice_business_type");
        jdbcTemplate.execute("""
                create table notice_business_type (
                    id bigint not null,
                    biz_type varchar(64) not null,
                    biz_name varchar(128) not null,
                    biz_group varchar(64),
                    domain_code varchar(64) not null default 'COMMON',
                    description varchar(500),
                    params_schema clob,
                    enabled boolean not null default true,
                    default_priority varchar(32) not null default 'NORMAL',
                    idempotent_strategy varchar(64),
                    tenant_id varchar(64) not null default 'default',
                    created_by bigint,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id),
                    unique (tenant_id, biz_type)
                )
                """);
        jdbcTemplate.execute("""
                create table notice_business_config_version (
                    id bigint not null,
                    business_type_id bigint not null,
                    biz_type varchar(64) not null,
                    params_schema clob,
                    default_priority varchar(32) not null default 'NORMAL',
                    idempotent_strategy varchar(64),
                    version int not null default 1,
                    version_status varchar(32) not null default 'DRAFT',
                    publish_time timestamp,
                    publish_by bigint,
                    tenant_id varchar(64) not null default 'default',
                    created_by bigint,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id),
                    unique (tenant_id, biz_type, version)
                )
                """);
        jdbcTemplate.execute("""
                create table notice_business_channel_template (
                    id bigint not null,
                    business_type_id bigint not null,
                    biz_type varchar(64) not null,
                    channel_type varchar(32) not null,
                    template_name varchar(128),
                    title_template varchar(200),
                    content_template clob,
                    channel_template_id varchar(128),
                    variable_mapping clob,
                    version int not null default 1,
                    version_status varchar(32) not null default 'DRAFT',
                    enabled boolean not null default true,
                    channel_config_id bigint,
                    publish_time timestamp,
                    publish_by bigint,
                    tenant_id varchar(64) not null default 'default',
                    created_by bigint,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id),
                    unique (tenant_id, biz_type, channel_type, version)
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

    private boolean booleanValue(String tableName, String columnName, String whereClause) {
        Boolean value = jdbcTemplate.queryForObject("select " + columnName + " from " + tableName + " where " + whereClause,
                Boolean.class);
        return Boolean.TRUE.equals(value);
    }

    @Configuration
    @Import(NoticeMessageTemplateResourceHandler.class)
    @MapperScan(basePackageClasses = {
            NoticeBusinessTypeMapper.class,
            NoticeBusinessConfigVersionMapper.class,
            NoticeBusinessChannelTemplateMapper.class
    })
    static class TestConfig {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MapNode(MangoNode mango) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MangoNode(ResourceNode resource) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResourceNode(Map<String, List<ResourceDeclaration>> declarations) {
    }
}
