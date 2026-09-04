package io.mango.notice.core.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mango.infra.bootstrap.api.BootstrapGenerationFence;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.kv.api.ILeaseLocker;
import io.mango.infra.kv.api.LockLease;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.notice.core.mapper.NoticeBusinessTypeMapper;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import io.mango.resource.api.command.ResourceModuleManifestCommand;
import io.mango.resource.api.enums.ResourceApplyMode;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.core.diagnostic.ResourceModuleSyncStatusRegistry;
import io.mango.resource.core.mapper.ResourceRegistryMapper;
import io.mango.resource.core.service.IResourceRegistryService;
import io.mango.resource.core.service.impl.ResourceRegistryService;
import io.mango.resource.core.sync.ResourceContentHasher;
import io.mango.resource.core.sync.ResourceModuleReceiptRepository;
import io.mango.resource.core.sync.ResourceRegistryLock;
import io.mango.resource.core.sync.ResourceRegistryRepository;
import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.ResourceTargetDispatcher;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.declaration.ResourceDeclarationLoader;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.support.model.ResourceSyncResult;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

@SpringBootTest(
        classes = {
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            PersistenceMybatisPlusAutoConfiguration.class,
            NoticeMessageTemplateResourceHandlerIntegrationTest.TestConfig.class
        })
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:notice_message_template_resource;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.flyway.enabled=false",
            "mango.persistence.mybatis-plus.tenant.enabled=true",
            "mango.persistence.mybatis-plus.tenant.default-tenant-id=1"
        })
class NoticeMessageTemplateResourceHandlerIntegrationTest {
    @Autowired private DataSource dataSource;

    @Autowired private NoticeMessageTemplateResourceHandler handler;
    @Autowired private IResourceRegistryService registryService;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ResourceContentHasher contentHasher;

    @BeforeEach
    void setUp() throws Exception {
        rebuildTables();
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void upsertCreatesMessageTemplatePackage() throws Exception {
        handler.upsert(messageTemplateDeclaration("定时任务执行失败：{{jobName}}", true));

        assertThat(count("notice_business_type")).isOne();
        assertThat(count("notice_business_config_version")).isOne();
        assertThat(count("notice_business_channel_template")).isOne();
        assertThat(stringValue("notice_business_type", "biz_type", "id = 2060000000000014001"))
                .isEqualTo("job.instance.failed");
        assertThat(
                        stringValue(
                                "notice_business_type",
                                "default_priority",
                                "id = 2060000000000014001"))
                .isEqualTo("HIGH");
        assertThat(
                        stringValue(
                                "notice_business_config_version",
                                "version_status",
                                "id = 2060000000000014002"))
                .isEqualTo("ACTIVE");
        assertThat(
                        stringValue(
                                "notice_business_channel_template",
                                "channel_type",
                                "id = 2060000000000014003"))
                .isEqualTo("SITE");
    }

    @Test
    void upsertUpdatesMessageTemplatePackage() throws Exception {
        handler.upsert(messageTemplateDeclaration("定时任务执行失败：{{jobName}}", true));

        handler.upsert(messageTemplateDeclaration("任务失败：{{jobName}}", true));

        assertThat(
                        stringValue(
                                "notice_business_channel_template",
                                "title_template",
                                "id = 2060000000000014003"))
                .isEqualTo("任务失败：{{jobName}}");
        assertThat(count("notice_business_channel_template")).isOne();
    }

    @Test
    void channelEnabledControlsTemplateWithoutDisablingBusinessType() throws Exception {
        ResourceDeclaration declaration = messageTemplateDeclaration("定时任务执行失败：{{jobName}}", true);
        field(declaration, "channelEnabled", ResourceFieldType.BOOLEAN, false);

        handler.upsert(declaration);

        assertThat(booleanValue("notice_business_type", "enabled", "id = 2060000000000014001"))
                .isTrue();
        assertThat(booleanValue(
                "notice_business_channel_template", "enabled", "id = 2060000000000014003"))
                .isFalse();
    }

    @Test
    void legacyDeclarationUsesEnabledForBothBusinessTypeAndChannel() throws Exception {
        handler.upsert(messageTemplateDeclaration("定时任务执行失败：{{jobName}}", false));

        assertThat(booleanValue("notice_business_type", "enabled", "id = 2060000000000014001"))
                .isFalse();
        assertThat(booleanValue(
                "notice_business_channel_template", "enabled", "id = 2060000000000014003"))
                .isFalse();
    }

    @Test
    void upsertUsesOnlyDeclaredPublishTime() throws Exception {
        ResourceDeclaration declaration = messageTemplateDeclaration("定时任务执行失败：{{jobName}}", true);

        handler.upsert(declaration);

        assertThat(timestampValue(
                        "notice_business_config_version",
                        "publish_time",
                        "id = 2060000000000014002"))
                .isNull();
        assertThat(timestampValue(
                        "notice_business_channel_template",
                        "publish_time",
                        "id = 2060000000000014003"))
                .isNull();

        LocalDateTime publishTime = LocalDateTime.of(2026, 8, 30, 9, 15, 0);
        field(declaration, "publishTime", ResourceFieldType.DATETIME, publishTime);
        handler.upsert(declaration);

        assertThat(timestampValue(
                        "notice_business_config_version",
                        "publish_time",
                        "id = 2060000000000014002"))
                .isEqualTo(publishTime);
        assertThat(timestampValue(
                        "notice_business_channel_template",
                        "publish_time",
                        "id = 2060000000000014003"))
                .isEqualTo(publishTime);
    }

    @Test
    void disableMarksBusinessAndTemplateDisabled() throws Exception {
        ResourceDeclaration declaration = messageTemplateDeclaration("定时任务执行失败：{{jobName}}", true);
        handler.upsert(declaration);

        handler.disable(declaration);

        assertThat(booleanValue("notice_business_type", "enabled", "id = 2060000000000014001"))
                .isFalse();
        assertThat(
                        booleanValue(
                                "notice_business_channel_template",
                                "enabled",
                                "id = 2060000000000014003"))
                .isFalse();
    }

    @Test
    void sparseDisableUsesRegistryTargetAndPreservesRuntimeChanges() throws Exception {
        ResourceDeclaration declaration = messageTemplateDeclaration("声明标题", true);
        handler.upsert(declaration);
        execute("update notice_business_channel_template set template_name = '运行时名称', "
                + "title_template = '运行时标题', content_template = '运行时内容' where id = 2060000000000014003");

        handler.disable(sparseTarget(2060000000000014003L, "notice_business_channel_template", "1"));

        assertThat(booleanValue("notice_business_channel_template", "enabled", "id = 2060000000000014003"))
                .isFalse();
        assertThat(stringValue("notice_business_channel_template", "template_name", "id = 2060000000000014003"))
                .isEqualTo("运行时名称");
        assertThat(stringValue("notice_business_channel_template", "title_template", "id = 2060000000000014003"))
                .isEqualTo("运行时标题");
        assertThat(stringValue("notice_business_channel_template", "content_template", "id = 2060000000000014003"))
                .isEqualTo("运行时内容");
    }

    @Test
    void sparseDeleteUsesRegistryTargetAndKeepsSharedRecords() throws Exception {
        ResourceDeclaration site = messageTemplateDeclaration("系统标题", true);
        ResourceDeclaration email = messageTemplateDeclaration("邮件标题", true);
        email.setBizKey("job.message.job-instance-failed-email");
        field(email, "channelTemplateId", ResourceFieldType.LONG, 2060000000000014004L);
        field(email, "channelType", ResourceFieldType.STRING, "EMAIL");
        field(email, "templateName", ResourceFieldType.STRING, "邮件模板");
        handler.upsert(site);
        handler.upsert(email);

        handler.delete(sparseTarget(2060000000000014004L, "notice_business_channel_template", "1"));

        assertThat(count("notice_business_channel_template")).isOne();
        assertThat(count("notice_business_config_version")).isOne();
        assertThat(count("notice_business_type")).isOne();
        assertThat(stringValue("notice_business_channel_template", "channel_type",
                "id = 2060000000000014003")).isEqualTo("SITE");
    }

    @Test
    void sparseLifecycleIsIdempotentWhenTargetWasDeleted() throws Exception {
        handler.upsert(messageTemplateDeclaration("系统标题", true));
        execute("delete from notice_business_channel_template where id = 2060000000000014003");

        handler.disable(sparseTarget(2060000000000014003L, "notice_business_channel_template", "1"));
        handler.delete(sparseTarget(2060000000000014003L, "notice_business_channel_template", "1"));

        assertThat(count("notice_business_channel_template")).isZero();
        assertThat(count("notice_business_config_version")).isOne();
        assertThat(count("notice_business_type")).isOne();
    }

    @Test
    void registryFinalizeMissingUsesSparseTargetAndRestoresAmbientTenant() throws Exception {
        ResourceDeclaration declaration = messageTemplateDeclaration("系统标题", true);
        declaration.setAppCode("notice-it");
        declaration.setServiceCode("notice-service");
        declaration.setSyncMode(io.mango.resource.api.enums.ResourceSyncMode.AUTO);
        handler.upsert(declaration);

        assertThat(registryService.registerDeclarations(registryCommand(1, List.of(declaration)))).isTrue();
        execute("update notice_business_channel_template set template_name = '运行时名称' "
                + "where id = 2060000000000014003");
        assertThat(registryService.registerDeclarations(registryCommand(2, List.of()))).isTrue();

        assertThat(booleanValue("notice_business_channel_template", "enabled",
                "id = 2060000000000014003")).isFalse();
        assertThat(stringValue("notice_business_channel_template", "template_name",
                "id = 2060000000000014003")).isEqualTo("运行时名称");
        assertThat(stringValue("resource_registry", "status",
                "resource_id = '2026061800700014001'")).isEqualTo("REMOVED");
        assertThat(io.mango.infra.context.api.MangoContextHolder.tenantId()).isEqualTo("1");
    }

    @Test
    void sparseDeleteSwitchesToRealTargetTenantWithoutTouchingCurrentTenant() throws Exception {
        handler.upsert(messageTemplateDeclaration("租户一标题", true));
        ResourceDeclaration tenantTwo = messageTemplateDeclaration("租户二标题", true);
        field(tenantTwo, "businessTypeId", ResourceFieldType.LONG, 2060000000000015001L);
        field(tenantTwo, "configVersionId", ResourceFieldType.LONG, 2060000000000015002L);
        field(tenantTwo, "channelTemplateId", ResourceFieldType.LONG, 2060000000000015003L);
        field(tenantTwo, "tenantId", ResourceFieldType.STRING, "2");
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previous.withTenantId("2"));
            handler.upsert(tenantTwo);
        } finally {
            MangoContextHolder.set(previous);
        }

        handler.delete(sparseTarget(2060000000000015003L, "notice_business_channel_template", "2"));

        assertThat(countForTenant("notice_business_channel_template", "1")).isOne();
        assertThat(countForTenant("notice_business_channel_template", "2")).isZero();
        assertThat(MangoContextHolder.tenantId()).isEqualTo("1");
    }

    @Test
    void sparseLifecycleRejectsUnexpectedTargetTable() throws Exception {
        handler.upsert(messageTemplateDeclaration("系统标题", true));

        assertThatThrownBy(() -> handler.disable(
                sparseTarget(2060000000000014003L, "notice_business_type", "1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("targetTable must match notice_business_channel_template");
    }

    @Test
    void sparseLifecycleRejectsTargetTenantMismatch() throws Exception {
        handler.upsert(messageTemplateDeclaration("系统标题", true));

        assertThatThrownBy(() -> handler.delete(
                sparseTarget(2060000000000014003L, "notice_business_channel_template", "2")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tenantId does not match target");
        assertThat(count("notice_business_channel_template")).isOne();
    }

    @Test
    void deleteRollsBackAllChangesWhenAssociatedBusinessTypeCannotBeDeleted() throws Exception {
        handler.upsert(messageTemplateDeclaration("系统标题", true));
        execute("create table notice_business_type_guard (business_type_id bigint not null, "
                + "constraint fk_notice_business_type_guard foreign key (business_type_id) "
                + "references notice_business_type(id))");
        execute("insert into notice_business_type_guard (business_type_id) values (2060000000000014001)");

        assertThatThrownBy(() -> handler.delete(
                sparseTarget(2060000000000014003L, "notice_business_channel_template", "1")))
                .isInstanceOf(RuntimeException.class);

        assertThat(count("notice_business_channel_template")).isOne();
        assertThat(count("notice_business_config_version")).isOne();
        assertThat(count("notice_business_type")).isOne();
    }

    @Test
    void upsertStillRequiresCompleteDeclarationFields() {
        ResourceDeclaration missingConfigVersion = messageTemplateDeclaration("系统标题", true);
        missingConfigVersion.getFields().get("configVersionId").setValue(null);
        assertThatThrownBy(() -> handler.upsert(missingConfigVersion))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("configVersionId");

        ResourceDeclaration missingChannelTemplateId = messageTemplateDeclaration("系统标题", true);
        missingChannelTemplateId.getFields().get("channelTemplateId").setValue(null);
        assertThatThrownBy(() -> handler.upsert(missingChannelTemplateId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("channelTemplateId");

        ResourceDeclaration missingContent = messageTemplateDeclaration("系统标题", true);
        missingContent.getFields().get("contentTemplate").setValue(null);
        assertThatThrownBy(() -> handler.upsert(missingContent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contentTemplate");
    }

    @Test
    void deletePhysicallyDeletesMessageTemplatePackage() throws Exception {
        ResourceDeclaration declaration = messageTemplateDeclaration("定时任务执行失败：{{jobName}}", true);
        handler.upsert(declaration);

        handler.delete(declaration);

        assertThat(count("notice_business_channel_template")).isZero();
        assertThat(count("notice_business_config_version")).isZero();
        assertThat(count("notice_business_type")).isZero();
    }

    @Test
    void deleteOneChannelKeepsSharedBusinessType() throws Exception {
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
        assertThat(
                        stringValue(
                                "notice_business_channel_template",
                                "channel_type",
                                "id = 2060000000000014003"))
                .isEqualTo("SITE");
    }

    @Test
    void jobMessageTemplateMatchesOldFlywaySeed() throws Exception {
        handler.upsert(jobInstanceFailedMessageTemplateDeclaration());

        assertThat(count("notice_business_type")).isOne();
        assertThat(count("notice_business_config_version")).isOne();
        assertThat(count("notice_business_channel_template")).isOne();
        assertThat(stringValue("notice_business_type", "biz_type", "id = 2060000000000014001"))
                .isEqualTo("job.instance.failed");
        assertThat(stringValue("notice_business_type", "biz_name", "id = 2060000000000014001"))
                .isEqualTo("定时任务执行失败");
        assertThat(stringValue("notice_business_type", "tenant_id", "id = 2060000000000014001"))
                .isEqualTo("1");
        assertThat(stringValue("notice_business_type", "params_schema", "id = 2060000000000014001"))
                .contains("\"required\":[\"jobCode\",\"jobName\",\"instanceId\",\"errorSummary\"]");
        assertThat(
                        intValue(
                                "notice_business_config_version",
                                "version",
                                "id = 2060000000000014002"))
                .isEqualTo(1);
        assertThat(
                        stringValue(
                                "notice_business_config_version",
                                "version_status",
                                "id = 2060000000000014002"))
                .isEqualTo("ACTIVE");
        assertThat(
                        stringValue(
                                "notice_business_channel_template",
                                "title_template",
                                "id = 2060000000000014003"))
                .isEqualTo("定时任务执行失败：{{jobName}}");
        assertThat(
                        stringValue(
                                "notice_business_channel_template",
                                "content_template",
                                "id = 2060000000000014003"))
                .contains("请进入平台能力/任务管理/执行实例查看日志");
    }

    private ResourceDeclaration jobInstanceFailedMessageTemplateDeclaration() {
        ResourceDeclaration declaration = messageTemplateDeclaration("定时任务执行失败：{{jobName}}", true);
        field(
                declaration,
                "paramsSchema",
                ResourceFieldType.JSON,
                "{\"type\":\"object\",\"required\":[\"jobCode\",\"jobName\",\"instanceId\",\"errorSummary\"]}");
        return declaration;
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
        field(
                declaration,
                "contentTemplate",
                ResourceFieldType.STRING,
                "定时任务 {{jobName}}（{{jobCode}}）执行失败。实例：{{instanceId}}；处理器：{{handlerName}}；触发批次：{{triggerBatchNo}}；失败原因：{{errorSummary}}。请进入平台能力/任务管理/执行实例查看日志。");
        field(declaration, "operatorId", ResourceFieldType.LONG, 1L);
        return declaration;
    }

    private ResourceDeclaration sparseTarget(Long targetId, String targetTable, String tenantId) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("2026061800700014999");
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.MESSAGE_TEMPLATE);
        declaration.setFields(new LinkedHashMap<>());
        field(declaration, "targetId", ResourceFieldType.LONG, targetId);
        field(declaration, "targetTable", ResourceFieldType.STRING, targetTable);
        field(declaration, "tenantId", ResourceFieldType.STRING, tenantId);
        return declaration;
    }

    private RegisterResourceDeclarationsCommand registryCommand(long generation,
                                                                 List<ResourceDeclaration> declarations)
            throws JsonProcessingException {
        ResourceModuleManifestCommand module = new ResourceModuleManifestCommand();
        module.setModuleCode("job");
        module.setDependencies(List.of());
        module.setDeclarations(objectMapper.writeValueAsString(declarations));
        module.setDeclarationCount(declarations.size());
        module.setModuleHash(contentHasher.moduleHash("job", List.of(), declarations));
        RegisterResourceDeclarationsCommand command = new RegisterResourceDeclarationsCommand();
        command.setAppCode("notice-it");
        command.setServiceCode("notice-service");
        command.setEnvironmentKey("notice-resource-test");
        command.setGeneration(generation);
        command.setManifestFingerprint("9".repeat(64));
        command.setFencingToken(generation);
        command.setApplyMode(ResourceApplyMode.FINALIZE);
        command.setModuleManifests(List.of(module));
        return command;
    }

    private void field(
            ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.putField(name, field);
    }

    private void rebuildTables() throws Exception {
        execute("drop table if exists resource_module_receipt");
        execute("drop table if exists resource_change_log");
        execute("drop table if exists resource_sync_log");
        execute("drop table if exists resource_registry");
        execute("drop table if exists notice_business_channel_template");
        execute("drop table if exists notice_business_config_version");
        execute("drop table if exists notice_business_type_guard");
        execute("drop table if exists notice_business_type");
        execute(
                """
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
        execute(
                """
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
        execute(
                """
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
                    route_mode varchar(16) not null default 'AUTO',
                    route_tag_code varchar(64),
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
        execute("""
                create table resource_registry (
                    id bigint primary key, resource_id varchar(64) not null,
                    resource_version int not null, app_code varchar(128) not null,
                    service_code varchar(128) not null, resource_type varchar(64) not null,
                    module_code varchar(64) not null, biz_key varchar(128) not null,
                    name varchar(128), target_module varchar(64) not null,
                    target_table varchar(128), target_id bigint, source_hash varchar(64),
                    sync_mode varchar(32) not null, status varchar(32) not null,
                    last_sync_time timestamp, tenant_id varchar(64), org_id bigint,
                    created_by bigint, created_at timestamp, updated_by bigint, updated_at timestamp
                )
                """);
        execute("create unique index uk_notice_resource_id on resource_registry(resource_id)");
        execute("create unique index uk_notice_resource_type_key on resource_registry(resource_type, biz_key)");
        execute("""
                create table resource_sync_log (
                    id bigint primary key, resource_id bigint, sync_type varchar(32) not null,
                    result varchar(32) not null, message clob, tenant_id varchar(64), org_id bigint,
                    created_by bigint, created_at timestamp, updated_by bigint, updated_at timestamp
                )
                """);
        execute("""
                create table resource_change_log (
                    id bigint primary key, resource_id bigint, change_type varchar(32) not null,
                    operator_id bigint, before_content clob, after_content clob, tenant_id varchar(64), org_id bigint,
                    created_by bigint, created_at timestamp, updated_by bigint, updated_at timestamp
                )
                """);
        execute("""
                create table resource_module_receipt (
                    environment_key varchar(128) not null, app_code varchar(128) not null,
                    service_code varchar(128) not null, module_code varchar(64) not null,
                    module_hash varchar(64) not null, generation bigint not null,
                    manifest_fingerprint varchar(64) not null, state varchar(32) not null,
                    declaration_count int not null, created_at timestamp default current_timestamp,
                    updated_at timestamp default current_timestamp,
                    primary key (environment_key, app_code, service_code, module_code)
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

    private long countForTenant(String tableName, String tenantId) throws Exception {
        try (Connection connection = dataSource.getConnection();
                var statement = connection.prepareStatement("select count(*) from " + tableName
                        + " where tenant_id = ?")) {
            statement.setString(1, tenantId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private String stringValue(String tableName, String columnName, String whereClause)
            throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                "select "
                                        + columnName
                                        + " from "
                                        + tableName
                                        + " where "
                                        + whereClause)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private int intValue(String tableName, String columnName, String whereClause) throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                "select "
                                        + columnName
                                        + " from "
                                        + tableName
                                        + " where "
                                        + whereClause)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private boolean booleanValue(String tableName, String columnName, String whereClause)
            throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                "select "
                                        + columnName
                                        + " from "
                                        + tableName
                                        + " where "
                                        + whereClause)) {
            resultSet.next();
            return resultSet.getBoolean(1);
        }
    }

    private LocalDateTime timestampValue(String tableName, String columnName, String whereClause)
            throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "select " + columnName + " from " + tableName + " where " + whereClause)) {
            resultSet.next();
            return resultSet.getObject(1, LocalDateTime.class);
        }
    }

    @Configuration
    @Import({NoticeMessageTemplateResourceHandler.class, ResourceRegistryRepository.class,
            ResourceModuleReceiptRepository.class, ResourceRegistryLock.class, ResourceRegistryService.class})
    @MapperScan(basePackageClasses = {NoticeBusinessTypeMapper.class, ResourceRegistryMapper.class})
    static class TestConfig {
        @Bean
        ResourceRegistryProperties resourceRegistryProperties() {
            ResourceRegistryProperties properties = new ResourceRegistryProperties();
            properties.setLocations(List.of());
            properties.setInstanceId("notice-resource-test");
            return properties;
        }

        @Bean
        ResourceDeclarationLoader resourceDeclarationLoader(ObjectMapper mapper,
                                                            ResourceRegistryProperties properties) {
            return new ResourceDeclarationLoader(mapper, properties);
        }

        @Bean
        ResourceDeclarationCollector resourceDeclarationCollector(ObjectProvider<ResourceProvider> providers,
                                                                  ResourceDeclarationLoader loader) {
            return new ResourceDeclarationCollector(providers);
        }

        @Bean
        ResourceContentHasher resourceContentHasher(ObjectMapper mapper) {
            return new ResourceContentHasher(mapper);
        }

        @Bean
        ResourceModuleSyncStatusRegistry resourceModuleSyncStatusRegistry(ResourceContentHasher hasher) {
            return new ResourceModuleSyncStatusRegistry(hasher);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        ILeaseLocker leaseLocker() {
            return new InMemoryLeaseLocker();
        }

        @Bean
        BootstrapGenerationFence bootstrapGenerationFence() {
            return authority -> { };
        }

        @Bean
        ResourceTargetDispatcher resourceTargetDispatcher() {
            return new NoopResourceTargetDispatcher();
        }
    }

    static class InMemoryLeaseLocker implements ILeaseLocker {
        private final Map<String, String> leases = new ConcurrentHashMap<>();

        @Override
        public Optional<LockLease> tryAcquire(String key, String owner, long ttlSeconds) {
            String token = owner + ":" + UUID.randomUUID();
            if (leases.putIfAbsent(key, token) != null) {
                return Optional.empty();
            }
            Instant acquiredAt = Instant.now();
            return Optional.of(new LockLease(key, owner, token, acquiredAt, acquiredAt.plusSeconds(ttlSeconds)));
        }

        @Override
        public Optional<LockLease> renew(LockLease lease, long ttlSeconds) {
            if (!leases.getOrDefault(lease.key(), "").equals(lease.token())) {
                return Optional.empty();
            }
            return Optional.of(lease.renewedUntil(Instant.now().plusSeconds(ttlSeconds)));
        }

        @Override
        public boolean release(LockLease lease) {
            return leases.remove(lease.key(), lease.token());
        }
    }

    static class NoopResourceTargetDispatcher implements ResourceTargetDispatcher {
        @Override public boolean supports(String targetModule) { return false; }
        @Override public Map<String, ResourceSyncResult> upsertBatch(List<ResourceDeclaration> declarations,
                                                                       List<ResourceDeclaration> completeBatch) {
            return Map.of();
        }
        @Override public ResourceSyncResult disable(ResourceDeclaration declaration) { return null; }
        @Override public ResourceSyncResult delete(ResourceDeclaration declaration) { return null; }
    }
}
