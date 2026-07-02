package io.mango.resource.core.sync;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.kv.api.ILocker;
import io.mango.infra.kv.core.capability.KvStoreLocker;
import io.mango.infra.kv.core.jdbc.JdbcKvStore;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.ResourceTargetDispatcher;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.api.enums.ResourceSyncMode;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceSyncResult;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.declaration.ResourceDeclarationLoader;
import io.mango.resource.core.entity.ResourceRegistryEntity;
import io.mango.resource.core.mapper.ResourceRegistryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        ResourceRegistrySyncServiceIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:resource_registry;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false",
        "mybatis-plus.mapper-locations=classpath:/mapper/resource/*.xml"
})
class ResourceRegistrySyncServiceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ResourceRegistrySyncService syncService;

    @Autowired
    private ResourceRegistryMapper registryMapper;

    @Autowired
    private MutableResourceProvider provider;

    @Autowired
    private TestMessageResourceHandler handler;

    @Autowired
    private RecordingResourceTargetDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        rebuildTables();
        provider.setDeclaration(activeDeclaration(1, "提交申请"));
        dispatcher.reset();
    }

    @Test
    void syncCreatesRegistryTargetResourceAndLogs() {
        syncService.sync();

        ResourceRegistryEntity registry = registryMapper.selectByResourceId("1900000000000000001");
        assertThat(registry).isNotNull();
        assertThat(registry.getAppCode()).isEqualTo("local");
        assertThat(registry.getServiceCode()).isEqualTo("local");
        assertThat(registry.getResourceVersion()).isEqualTo(1);
        assertThat(registry.getResourceType()).isEqualTo("MESSAGE_TEMPLATE");
        assertThat(registry.getBizKey()).isEqualTo("guarantee.apply.submit");
        assertThat(registry.getTargetId()).isEqualTo(91001L);

        assertThat(count("resource_sync_log")).isEqualTo(1);
        assertThat(count("resource_change_log")).isEqualTo(1);
        assertThat(stringValue("message_template", "title")).isEqualTo("提交申请");
        assertThat(dispatcher.upsertBatchCount()).isZero();
    }

    @Test
    void syncUsesRemoteDispatcherWhenLocalHandlerIsMissing() {
        provider.setDeclaration(remoteOnlyDeclaration());

        syncService.sync();

        ResourceRegistryEntity registry = registryMapper.selectByResourceId("1900000000000000002");
        assertThat(registry).isNotNull();
        assertThat(registry.getTargetId()).isEqualTo(92001L);
        assertThat(registry.getTargetTable()).isEqualTo("remote_notice_template");
        assertThat(dispatcher.upsertBatchCount()).isEqualTo(1);
    }

    @Test
    void syncPrefersLocalHandlerWhenRemoteDispatcherAlsoSupportsTargetModule() {
        syncService.sync();

        assertThat(stringValue("message_template", "title")).isEqualTo("提交申请");
        assertThat(dispatcher.upsertBatchCount()).isZero();
    }

    @Test
    void syncUpdatesAutoResourceWhenHashChanges() {
        syncService.sync();
        provider.setDeclaration(activeDeclaration(2, "提交申请新版"));

        syncService.sync();

        ResourceRegistryEntity registry = registryMapper.selectByResourceId("1900000000000000001");
        assertThat(registry.getResourceVersion()).isEqualTo(2);
        assertThat(count("resource_sync_log")).isEqualTo(2);
        assertThat(count("resource_change_log")).isEqualTo(2);
        assertThat(stringValue("message_template", "title")).isEqualTo("提交申请新版");
    }

    @Test
    void syncCreatesInitOnlyResourceOnFirstSync() {
        ResourceDeclaration declaration = activeDeclaration(1, "初始化标题");
        declaration.setSyncMode(ResourceSyncMode.INIT_ONLY);
        provider.setDeclaration(declaration);

        syncService.sync();

        ResourceRegistryEntity registry = registryMapper.selectByResourceId("1900000000000000001");
        assertThat(registry.getResourceVersion()).isEqualTo(1);
        assertThat(registry.getSyncMode()).isEqualTo("INIT_ONLY");
        assertThat(stringValue("message_template", "title")).isEqualTo("初始化标题");
    }

    @Test
    void syncAutoAndInitOnlyFirstSyncWriteSameTargetRowsForFiveDeclarations() {
        provider.setDeclarations(fiveDeclarations(ResourceSyncMode.AUTO, 1, "初始化标题"));
        syncService.sync();
        List<MessageTemplateRow> autoRows = messageTemplateRows();

        rebuildTables();
        dispatcher.reset();
        provider.setDeclarations(fiveDeclarations(ResourceSyncMode.INIT_ONLY, 1, "初始化标题"));
        syncService.sync();

        assertThat(messageTemplateRows()).containsExactlyElementsOf(autoRows);
        assertThat(registryRows()).containsExactly(
                new RegistryRow("1900000000000000001", 1, "guarantee.apply.case-1", "INIT_ONLY", "ACTIVE", 91001L),
                new RegistryRow("1900000000000000002", 1, "guarantee.apply.case-2", "INIT_ONLY", "ACTIVE", 91002L),
                new RegistryRow("1900000000000000003", 1, "guarantee.apply.case-3", "INIT_ONLY", "ACTIVE", 91003L),
                new RegistryRow("1900000000000000004", 1, "guarantee.apply.case-4", "INIT_ONLY", "ACTIVE", 91004L),
                new RegistryRow("1900000000000000005", 1, "guarantee.apply.case-5", "INIT_ONLY", "ACTIVE", 91005L)
        );
    }

    @Test
    void syncKeepsRuntimeTargetRowsForFiveInitOnlyDeclarationsWhenUpgraded() {
        provider.setDeclarations(fiveDeclarations(ResourceSyncMode.INIT_ONLY, 1, "初始化标题"));
        syncService.sync();
        jdbcTemplate.update("update message_template set title = '运行时修改-1' where id = 91001");
        jdbcTemplate.update("update message_template set title = '运行时修改-2' where id = 91002");
        jdbcTemplate.update("update message_template set title = '运行时修改-3' where id = 91003");
        jdbcTemplate.update("update message_template set title = '运行时修改-4' where id = 91004");
        jdbcTemplate.update("update message_template set title = '运行时修改-5' where id = 91005");
        List<MessageTemplateRow> runtimeRows = messageTemplateRows();

        provider.setDeclarations(fiveDeclarations(ResourceSyncMode.INIT_ONLY, 2, "升级标题"));
        syncService.sync();

        assertThat(messageTemplateRows()).containsExactlyElementsOf(runtimeRows);
        assertThat(registryRows()).containsExactly(
                new RegistryRow("1900000000000000001", 2, "guarantee.apply.case-1", "INIT_ONLY", "ACTIVE", 91001L),
                new RegistryRow("1900000000000000002", 2, "guarantee.apply.case-2", "INIT_ONLY", "ACTIVE", 91002L),
                new RegistryRow("1900000000000000003", 2, "guarantee.apply.case-3", "INIT_ONLY", "ACTIVE", 91003L),
                new RegistryRow("1900000000000000004", 2, "guarantee.apply.case-4", "INIT_ONLY", "ACTIVE", 91004L),
                new RegistryRow("1900000000000000005", 2, "guarantee.apply.case-5", "INIT_ONLY", "ACTIVE", 91005L)
        );
        assertThat(jdbcTemplate.queryForList(
                "select sync_type from resource_sync_log order by id", String.class))
                .containsExactly("CREATE", "CREATE", "CREATE", "CREATE", "CREATE",
                        "SKIP", "SKIP", "SKIP", "SKIP", "SKIP");
    }

    @Test
    void syncKeepsRuntimeTargetWhenInitOnlyDeclarationChanges() {
        ResourceDeclaration first = activeDeclaration(1, "初始化标题");
        first.setSyncMode(ResourceSyncMode.INIT_ONLY);
        provider.setDeclaration(first);
        syncService.sync();
        jdbcTemplate.update("update message_template set title = '运行时修改' where id = 91001");

        ResourceDeclaration upgraded = activeDeclaration(2, "升级包标题");
        upgraded.setSyncMode(ResourceSyncMode.INIT_ONLY);
        provider.setDeclaration(upgraded);
        syncService.sync();

        ResourceRegistryEntity registry = registryMapper.selectByResourceId("1900000000000000001");
        assertThat(registry.getResourceVersion()).isEqualTo(2);
        assertThat(registry.getSyncMode()).isEqualTo("INIT_ONLY");
        assertThat(stringValue("message_template", "title")).isEqualTo("运行时修改");
        assertThat(count("resource_sync_log")).isEqualTo(2);
        assertThat(count("resource_change_log")).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "select sync_type from resource_sync_log order by created_at desc limit 1", String.class))
                .isEqualTo("SKIP");
    }

    @Test
    void syncRejectsResourceVersionRollback() {
        provider.setDeclaration(activeDeclaration(2, "提交申请新版"));
        syncService.sync();
        provider.setDeclaration(activeDeclaration(1, "回退版本"));

        assertThatThrownBy(() -> syncService.sync())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("version rollback is not allowed");

        ResourceRegistryEntity registry = registryMapper.selectByResourceId("1900000000000000001");
        assertThat(registry.getResourceVersion()).isEqualTo(2);
        assertThat(stringValue("message_template", "title")).isEqualTo("提交申请新版");
    }

    @Test
    void syncUpdatesRegistrySyncModeWhenDeclarationChanges() {
        syncService.sync();
        ResourceDeclaration manualDeclaration = activeDeclaration(2, "提交申请");
        manualDeclaration.setSyncMode(ResourceSyncMode.MANUAL);
        provider.setDeclaration(manualDeclaration);

        syncService.sync();

        ResourceRegistryEntity registry = registryMapper.selectByResourceId("1900000000000000001");
        assertThat(registry.getResourceVersion()).isEqualTo(2);
        assertThat(registry.getSyncMode()).isEqualTo("MANUAL");
    }

    @Test
    void syncSkipsManualResourceWhenProviderChanges() {
        syncService.sync();
        jdbcTemplate.update("update resource_registry set sync_mode = 'MANUAL' where resource_id = '1900000000000000001'");
        provider.setDeclaration(activeDeclaration(2, "人工接管后不覆盖"));

        syncService.sync();

        ResourceRegistryEntity registry = registryMapper.selectByResourceId("1900000000000000001");
        assertThat(registry.getResourceVersion()).isEqualTo(1);
        assertThat(stringValue("message_template", "title")).isEqualTo("提交申请");
        assertThat(count("resource_sync_log")).isEqualTo(2);
    }

    @Test
    void syncDisablesMissingAutoResource() {
        syncService.sync();
        provider.clear();

        syncService.sync();

        ResourceRegistryEntity registry = registryMapper.selectByResourceId("1900000000000000001");
        assertThat(registry.getStatus()).isEqualTo("REMOVED");
        assertThat(intValue("message_template", "enabled")).isZero();
    }

    @Test
    void remoteSyncDisablesMissingOnlyWithinSameSourceService() {
        ResourceDeclaration serviceA = activeDeclaration(1, "服务A");
        serviceA.setId("1900000000000000003");
        serviceA.setBizKey("guarantee.apply.service-a");
        ResourceDeclaration serviceB = activeDeclaration(1, "服务B");
        serviceB.setId("1900000000000000004");
        serviceB.setBizKey("guarantee.apply.service-b");

        syncService.syncRemote("platform-admin", "service-a", List.of(serviceA));
        syncService.syncRemote("platform-admin", "service-b", List.of(serviceB));
        syncService.syncRemote("platform-admin", "service-a", List.of("guarantee"), List.of());

        ResourceRegistryEntity registryA = registryMapper.selectByResourceId("1900000000000000003");
        ResourceRegistryEntity registryB = registryMapper.selectByResourceId("1900000000000000004");
        assertThat(registryA.getStatus()).isEqualTo("REMOVED");
        assertThat(registryB.getStatus()).isEqualTo("ACTIVE");
        assertThat(registryB.getAppCode()).isEqualTo("platform-admin");
        assertThat(registryB.getServiceCode()).isEqualTo("service-b");
    }

    @Test
    void remoteSyncAllowsNullDeclarationsWhenModuleCodesAreProvided() {
        ResourceDeclaration serviceA = activeDeclaration(1, "服务A");
        serviceA.setId("1900000000000000003");
        serviceA.setBizKey("guarantee.apply.service-a");
        syncService.syncRemote("platform-admin", "service-a", List.of(serviceA));

        syncService.syncRemote("platform-admin", "service-a", List.of("guarantee"), null);

        ResourceRegistryEntity registry = registryMapper.selectByResourceId("1900000000000000003");
        assertThat(registry.getStatus()).isEqualTo("REMOVED");
        assertThat(intValue("message_template", "enabled")).isZero();
    }

    @Test
    void syncFailsWhenMissingAutoResourceHasNoLocalHandlerOrRemoteDispatcher() {
        jdbcTemplate.update("""
                insert into resource_registry (
                    id, resource_id, resource_version, app_code, service_code, resource_type,
                    module_code, biz_key, name, target_module, target_table, target_id,
                    source_hash, sync_mode, status
                ) values (
                    90005, '1900000000000000005', 1, 'platform-admin', 'orphan-service', 'UNSUPPORTED_TEMPLATE',
                    'guarantee', 'guarantee.unsupported.submit', '未知远程模板', 'unsupported-notice',
                    'unsupported_template', 95001, 'old-hash', 'AUTO', 'ACTIVE'
                )
                """);

        assertThatThrownBy(() -> syncService.syncRemote(
                "platform-admin", "orphan-service", List.of("guarantee"), List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No resource handler found for missing resource disable")
                .hasMessageContaining("UNSUPPORTED_TEMPLATE")
                .hasMessageContaining("unsupported-notice");
    }

    @Test
    void syncDeprecatedDeclarationKeepsTargetReadable() {
        syncService.sync();
        ResourceDeclaration deprecated = activeDeclaration(2, "废弃声明不覆盖目标");
        deprecated.setStatus(ResourceStatus.DEPRECATED);
        provider.setDeclaration(deprecated);

        syncService.sync();

        ResourceRegistryEntity registry = registryMapper.selectByResourceId("1900000000000000001");
        assertThat(registry.getResourceVersion()).isEqualTo(2);
        assertThat(registry.getStatus()).isEqualTo("DEPRECATED");
        assertThat(stringValue("message_template", "title")).isEqualTo("提交申请");
        assertThat(intValue("message_template", "enabled")).isEqualTo(1);
    }

    @Test
    void forceSyncRebuildsTargetWhenRegistryIsUnchanged() {
        syncService.sync();
        jdbcTemplate.update("delete from message_template");

        syncService.sync();

        assertThat(count("message_template")).isZero();

        syncService.sync(true);

        assertThat(count("message_template")).isEqualTo(1);
        assertThat(stringValue("message_template", "title")).isEqualTo("提交申请");
        assertThat(count("resource_registry")).isEqualTo(1);
    }

    @Test
    void deleteResourcePhysicallyDeletesTargetAndKeepsRegistryRemoved() {
        syncService.sync();

        syncService.deleteResource("1900000000000000001", true);

        ResourceRegistryEntity registry = registryMapper.selectByResourceId("1900000000000000001");
        assertThat(registry.getStatus()).isEqualTo("REMOVED");
        assertThat(count("message_template")).isZero();
        assertThat(count("resource_sync_log")).isEqualTo(2);
        assertThat(count("resource_change_log")).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "select sync_type from resource_sync_log order by created_at desc limit 1", String.class))
                .isEqualTo("DELETE");
        assertThat(jdbcTemplate.queryForObject(
                "select change_type from resource_change_log order by created_at desc limit 1", String.class))
                .isEqualTo("DELETE");
    }

    private ResourceDeclaration activeDeclaration(int version, String titleValue) {
        return activeDeclaration("1900000000000000001", version, "guarantee.apply.submit", titleValue);
    }

    private ResourceDeclaration activeDeclaration(String id, int version, String bizKey, String titleValue) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(id);
        declaration.setVersion(version);
        declaration.setResourceType("MESSAGE_TEMPLATE");
        declaration.setModuleCode("guarantee");
        declaration.setBizKey(bizKey);
        declaration.setName("提交申请通知");
        declaration.setTargetModule("notice");
        declaration.setFields(new LinkedHashMap<>());
        ResourceField title = new ResourceField();
        title.setType(ResourceFieldType.STRING);
        title.setValue(titleValue);
        declaration.getFields().put("title", title);
        return declaration;
    }

    private List<ResourceDeclaration> fiveDeclarations(ResourceSyncMode syncMode, int version, String titlePrefix) {
        return List.of(
                declarationWithMode("1900000000000000001", version, "guarantee.apply.case-1", titlePrefix + "-1",
                        syncMode),
                declarationWithMode("1900000000000000002", version, "guarantee.apply.case-2", titlePrefix + "-2",
                        syncMode),
                declarationWithMode("1900000000000000003", version, "guarantee.apply.case-3", titlePrefix + "-3",
                        syncMode),
                declarationWithMode("1900000000000000004", version, "guarantee.apply.case-4", titlePrefix + "-4",
                        syncMode),
                declarationWithMode("1900000000000000005", version, "guarantee.apply.case-5", titlePrefix + "-5",
                        syncMode)
        );
    }

    private ResourceDeclaration declarationWithMode(String id, int version, String bizKey, String titleValue,
                                                    ResourceSyncMode syncMode) {
        ResourceDeclaration declaration = activeDeclaration(id, version, bizKey, titleValue);
        declaration.setSyncMode(syncMode);
        return declaration;
    }

    private ResourceDeclaration remoteOnlyDeclaration() {
        ResourceDeclaration declaration = activeDeclaration(1, "远程模板");
        declaration.setId("1900000000000000002");
        declaration.setResourceType("REMOTE_TEMPLATE");
        declaration.setBizKey("guarantee.remote.submit");
        declaration.setTargetModule("remote-notice");
        return declaration;
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists resource_change_log");
        jdbcTemplate.execute("drop table if exists resource_sync_log");
        jdbcTemplate.execute("drop table if exists resource_registry");
        jdbcTemplate.execute("drop table if exists message_template");
        jdbcTemplate.execute("drop table if exists infra_kv_entry");
        jdbcTemplate.execute("""
                create table resource_registry (
                    id bigint primary key,
                    resource_id varchar(64) not null,
                    resource_version int not null,
                    app_code varchar(128) not null default 'local',
                    service_code varchar(128) not null default 'local',
                    resource_type varchar(64) not null,
                    module_code varchar(64) not null,
                    biz_key varchar(128) not null,
                    name varchar(128),
                    target_module varchar(64) not null,
                    target_table varchar(128),
                    target_id bigint,
                    source_hash varchar(64),
                    sync_mode varchar(32) not null,
                    status varchar(32) not null,
                    last_sync_time timestamp,
                    created_by bigint,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp
                )
                """);
        jdbcTemplate.execute("create unique index uk_resource_registry_resource_id on resource_registry(resource_id)");
        jdbcTemplate.execute("create unique index uk_resource_registry_type_biz_key on resource_registry(resource_type, biz_key)");
        jdbcTemplate.execute("""
                create table resource_sync_log (
                    id bigint primary key,
                    resource_id bigint,
                    sync_type varchar(32) not null,
                    result varchar(32) not null,
                    message clob,
                    created_at timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table resource_change_log (
                    id bigint primary key,
                    resource_id bigint,
                    change_type varchar(32) not null,
                    operator_id bigint,
                    before_content clob,
                    after_content clob,
                    created_at timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table message_template (
                    id bigint primary key,
                    biz_key varchar(128) not null,
                    title varchar(128),
                    enabled int not null
                )
                """);
        jdbcTemplate.execute("""
                create table infra_kv_entry (
                    id          bigint not null,
                    kv_key      varchar(200) not null,
                    kv_value    text,
                    expire_time datetime not null,
                    create_time datetime not null default current_timestamp,
                    primary key (id),
                    unique key uk_kv_key (kv_key)
                )
                """);
    }

    private long count(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    private String stringValue(String tableName, String columnName) {
        return jdbcTemplate.queryForObject("select " + columnName + " from " + tableName + " limit 1", String.class);
    }

    private int intValue(String tableName, String columnName) {
        Integer value = jdbcTemplate.queryForObject("select " + columnName + " from " + tableName + " limit 1", Integer.class);
        return value == null ? 0 : value;
    }

    private List<MessageTemplateRow> messageTemplateRows() {
        return jdbcTemplate.query("""
                select id, biz_key, title, enabled
                from message_template
                order by id
                """, (rs, rowNum) -> new MessageTemplateRow(
                rs.getLong("id"),
                rs.getString("biz_key"),
                rs.getString("title"),
                rs.getInt("enabled")
        ));
    }

    private List<RegistryRow> registryRows() {
        return jdbcTemplate.query("""
                select resource_id, resource_version, biz_key, sync_mode, status, target_id
                from resource_registry
                order by resource_id
                """, (rs, rowNum) -> new RegistryRow(
                rs.getString("resource_id"),
                rs.getInt("resource_version"),
                rs.getString("biz_key"),
                rs.getString("sync_mode"),
                rs.getString("status"),
                rs.getLong("target_id")
        ));
    }

    @Configuration
    @MapperScan(basePackageClasses = {
            ResourceRegistryMapper.class,
            TestMessageTemplateMapper.class
    })
    @Import({ResourceRegistryRepository.class, ResourceRegistryLock.class, ResourceRegistrySyncService.class})
    static class TestConfig {

        @Bean
        ResourceRegistryProperties resourceRegistryProperties() {
            ResourceRegistryProperties properties = new ResourceRegistryProperties();
            properties.setLocations(List.of());
            properties.setInstanceId("resource-test");
            return properties;
        }

        @Bean
        ResourceDeclarationLoader resourceDeclarationLoader(ObjectMapper objectMapper,
                                                            ResourceRegistryProperties properties) {
            return new ResourceDeclarationLoader(objectMapper, properties);
        }

        @Bean
        ResourceDeclarationCollector resourceDeclarationCollector(ObjectProvider<ResourceProvider> providers,
                                                                  ResourceDeclarationLoader loader) {
            return new ResourceDeclarationCollector(providers);
        }

        @Bean
        ResourceContentHasher resourceContentHasher(ObjectMapper objectMapper) {
            return new ResourceContentHasher(objectMapper);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        ILocker locker(JdbcTemplate jdbcTemplate) {
            return new KvStoreLocker(new JdbcKvStore(jdbcTemplate));
        }

        @Bean
        MutableResourceProvider mutableResourceProvider() {
            return new MutableResourceProvider();
        }

        @Bean
        TestMessageResourceHandler testMessageResourceHandler(TestMessageTemplateMapper messageTemplateMapper) {
            return new TestMessageResourceHandler(messageTemplateMapper);
        }

        @Bean
        RecordingResourceTargetDispatcher recordingResourceTargetDispatcher() {
            return new RecordingResourceTargetDispatcher();
        }
    }

    static class MutableResourceProvider implements ResourceProvider {

        private final AtomicReference<List<ResourceDeclaration>> declarations = new AtomicReference<>(List.of());

        void setDeclaration(ResourceDeclaration declaration) {
            this.declarations.set(List.of(declaration));
        }

        void setDeclarations(List<ResourceDeclaration> declarations) {
            this.declarations.set(List.copyOf(declarations));
        }

        void clear() {
            this.declarations.set(List.of());
        }

        @Override
        public List<String> moduleCodes() {
            return List.of("guarantee");
        }

        @Override
        public List<ResourceDeclaration> provide() {
            return declarations.get();
        }
    }

    static class TestMessageResourceHandler implements ResourceHandler {

        private final TestMessageTemplateMapper messageTemplateMapper;

        TestMessageResourceHandler(TestMessageTemplateMapper messageTemplateMapper) {
            this.messageTemplateMapper = messageTemplateMapper;
        }

        @Override
        public String resourceType() {
            return "MESSAGE_TEMPLATE";
        }

        @Override
        public ResourceSyncResult upsert(ResourceDeclaration resource) {
            Long id = targetId(resource);
            String title = String.valueOf(resource.getFields().get("title").getValue());
            TestMessageTemplateEntity entity = messageTemplateMapper.selectById(id);
            if (entity == null) {
                entity = new TestMessageTemplateEntity();
                entity.setId(id);
                entity.setBizKey(resource.getBizKey());
                entity.setTitle(title);
                entity.setEnabled(1);
                messageTemplateMapper.insert(entity);
            } else {
                entity.setTitle(title);
                entity.setEnabled(1);
                messageTemplateMapper.updateById(entity);
            }
            return ResourceSyncResult.of(id, "message_template", "ok");
        }

        @Override
        public ResourceSyncResult disable(ResourceDeclaration resource) {
            Long id = targetId(resource);
            TestMessageTemplateEntity entity = messageTemplateMapper.selectById(id);
            if (entity != null) {
                entity.setEnabled(0);
                messageTemplateMapper.updateById(entity);
            }
            return ResourceSyncResult.of(id, "message_template", "disabled");
        }

        @Override
        public ResourceSyncResult delete(ResourceDeclaration resource) {
            Long id = targetId(resource);
            messageTemplateMapper.deleteById(id);
            return ResourceSyncResult.of(id, "message_template", "deleted");
        }

        private Long targetId(ResourceDeclaration resource) {
            String id = resource.getId();
            return 91000L + Long.parseLong(id.substring(id.length() - 4));
        }
    }

    static class RecordingResourceTargetDispatcher implements ResourceTargetDispatcher {

        private int upsertBatchCount;

        void reset() {
            upsertBatchCount = 0;
        }

        int upsertBatchCount() {
            return upsertBatchCount;
        }

        @Override
        public boolean supports(String targetModule) {
            return "notice".equals(targetModule) || "remote-notice".equals(targetModule);
        }

        @Override
        public Map<String, ResourceSyncResult> upsertBatch(List<ResourceDeclaration> declarations,
                                                           List<ResourceDeclaration> completeBatch) {
            upsertBatchCount++;
            ResourceDeclaration declaration = declarations.get(0);
            return Map.of(declaration.getId(),
                    ResourceSyncResult.of(92001L, "remote_notice_template", "remote ok"));
        }

        @Override
        public ResourceSyncResult disable(ResourceDeclaration declaration) {
            return ResourceSyncResult.of(92001L, "remote_notice_template", "remote disabled");
        }

        @Override
        public ResourceSyncResult delete(ResourceDeclaration declaration) {
            return ResourceSyncResult.of(92001L, "remote_notice_template", "remote deleted");
        }
    }
}

@Mapper
interface TestMessageTemplateMapper extends BaseMapper<TestMessageTemplateEntity> {
}

@TableName("message_template")
class TestMessageTemplateEntity {

    @TableId
    private Long id;

    private String bizKey;

    private String title;

    private Integer enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBizKey() {
        return bizKey;
    }

    public void setBizKey(String bizKey) {
        this.bizKey = bizKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }
}

record MessageTemplateRow(Long id, String bizKey, String title, Integer enabled) {
}

record RegistryRow(String resourceId, Integer resourceVersion, String bizKey, String syncMode, String status,
                   Long targetId) {
}
