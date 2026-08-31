package io.mango.resource.core.service.impl;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.common.exception.DependencyNotReadyException;
import io.mango.infra.kv.api.ILocker;
import io.mango.infra.bootstrap.api.BootstrapGenerationFence;
import io.mango.infra.kv.api.ILeaseLocker;
import io.mango.infra.kv.core.capability.KvStoreLocker;
import io.mango.infra.kv.core.capability.KvStoreLeaseLocker;
import io.mango.infra.kv.core.jdbc.JdbcKvStore;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.ResourceBaselinePolicy;
import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.ResourceTargetDispatcher;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import io.mango.resource.api.command.ResourceModuleManifestCommand;
import io.mango.resource.api.enums.ResourceApplyMode;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.api.enums.ResourceSyncMode;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.support.model.ResourceSyncContext;
import io.mango.resource.support.model.ResourceSyncResult;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.declaration.ResourceDeclarationLoader;
import io.mango.resource.core.entity.ResourceRegistryEntity;
import io.mango.resource.core.diagnostic.ResourceModuleSyncState;
import io.mango.resource.core.diagnostic.ResourceModuleSyncStatusRegistry;
import io.mango.resource.core.mapper.ResourceRegistryMapper;
import io.mango.resource.core.sync.ResourceContentHasher;
import io.mango.resource.core.sync.ResourceRegistryLock;
import io.mango.resource.core.sync.ResourceRegistryRepository;
import io.mango.resource.core.sync.ResourceModuleReceiptRepository;
import org.apache.ibatis.executor.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    private ResourceRegistryService syncService;

    @Autowired
    private ResourceRegistryMapper registryMapper;

    @Autowired
    private ILocker locker;

    @Autowired
    private MutableResourceProvider provider;

    @Autowired
    private TestMessageResourceHandler handler;

    @Autowired
    private RecordingResourceTargetDispatcher dispatcher;

    @Autowired
    private ResourceSyncOrderRecorder syncOrderRecorder;

    @Autowired
    private DeferredResourceHandler deferredResourceHandler;

    @Autowired
    private SqlStatementCounter sqlStatementCounter;

    @Autowired
    private ResourceModuleSyncStatusRegistry moduleSyncStatusRegistry;

    @Autowired
    private ResourceContentHasher resourceContentHasher;

    @Autowired
    private ResourceRegistryProperties resourceRegistryProperties;

    @BeforeEach
    void setUp() {
        syncService.start();
        rebuildTables();
        provider.setDeclaration(activeDeclaration(1, "提交申请"));
        handler.resetBlocking();
        handler.resetUpsertCount();
        handler.resetPreservation();
        handler.setEnvironmentRequired(false);
        resourceRegistryProperties.setBaselineBuildEnabled(false);
        dispatcher.reset();
        syncOrderRecorder.clear();
        sqlStatementCounter.reset();
    }

    @Test
    void portableBaselineOmitsEnvironmentRequiredHandlersWithoutCreatingRegistryState() {
        resourceRegistryProperties.setBaselineBuildEnabled(true);
        handler.setEnvironmentRequired(true);

        syncService.sync();

        assertThat(handler.upsertCount()).isZero();
        assertThat(count("message_template")).isZero();
        assertThat(count("resource_registry")).isZero();
        assertThat(count("resource_sync_log")).isZero();
        assertThat(count("resource_change_log")).isZero();

        resourceRegistryProperties.setBaselineBuildEnabled(false);
        syncService.sync();

        assertThat(handler.upsertCount()).isEqualTo(1);
        assertThat(count("message_template")).isEqualTo(1);
        assertThat(count("resource_registry")).isEqualTo(1);
    }

    @Test
    void shutdownBarrierWaitsForInFlightSyncAndRejectsRegistryWritesAfterStop() throws Exception {
        handler.blockNextUpsert();
        AtomicReference<RuntimeException> syncFailure = new AtomicReference<>();
        Thread syncThread = new Thread(() -> {
            try {
                syncService.sync();
            } catch (RuntimeException exception) {
                syncFailure.set(exception);
            }
        }, "resource-sync-test");
        syncThread.start();
        assertThat(handler.awaitBlockedUpsert(5, TimeUnit.SECONDS)).isTrue();

        CountDownLatch stopped = new CountDownLatch(1);
        syncService.stop(stopped::countDown);

        assertThat(stopped.await(200, TimeUnit.MILLISECONDS)).isFalse();
        assertThat(syncService.syncRemote(
                "platform-admin", "service-after-stop", List.of(activeDeclaration(1, "停机后资源"))))
                .isFalse();
        handler.releaseBlockedUpsert();
        assertThat(stopped.await(5, TimeUnit.SECONDS)).isTrue();
        syncThread.join(5000);

        assertThat(syncThread.isAlive()).isFalse();
        assertThat(syncFailure.get())
                .isInstanceOf(BizException.class)
                .hasMessageContaining("shutting down");
        assertThat(count("resource_registry")).isZero();
        assertThat(count("resource_sync_log")).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM infra_kv_entry WHERE kv_key = ?",
                Long.class,
                ResourceRegistryLock.LOCK_NAME)).isZero();
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
    void collectorFailureReplacesPreviousPassInsteadOfLeavingStaleSuccess() {
        syncService.sync();
        assertThat(moduleSyncStatusRegistry.resolve("guarantee")).get()
                .extracting(status -> status.state())
                .isEqualTo(ResourceModuleSyncState.APPLIED);
        provider.failWith(new IllegalStateException("collector failed"));

        assertThatThrownBy(syncService::sync)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("collector failed");

        assertThat(moduleSyncStatusRegistry.resolve("guarantee")).get().satisfies(status -> {
            assertThat(status.state()).isEqualTo(ResourceModuleSyncState.FAILED);
            assertThat(status.reasonCode()).isEqualTo("RESOURCE_SYNC_FAILED");
        });
    }

    @Test
    void validationAndMissingHandlerFailuresAreObserved() {
        ResourceDeclaration duplicate = activeDeclaration(1, "重复声明");
        provider.setDeclarations(List.of(activeDeclaration(1, "提交申请"), duplicate));

        assertThatThrownBy(syncService::sync).isInstanceOf(BizException.class);
        assertThat(moduleSyncStatusRegistry.resolve("guarantee")).get()
                .extracting(status -> status.state())
                .isEqualTo(ResourceModuleSyncState.FAILED);

        ResourceDeclaration unsupported = genericDeclaration(
                "1900000000000000099", "UNSUPPORTED", "guarantee.unsupported", "missing-target");
        provider.setDeclaration(unsupported);
        assertThatThrownBy(syncService::sync)
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未找到资源处理器");
        assertThat(moduleSyncStatusRegistry.resolve("guarantee")).get()
                .extracting(status -> status.state())
                .isEqualTo(ResourceModuleSyncState.FAILED);
    }

    @Test
    void diagnosticSnapshotFailureDoesNotReverseSuccessfulSynchronization() {
        sqlStatementCounter.failOnInvocation(ResourceRegistryMapper.class, "selectByResourceIds", 2);

        syncService.sync();

        assertThat(count("resource_registry")).isEqualTo(1);
        assertThat(count("message_template")).isEqualTo(1);
        assertThat(moduleSyncStatusRegistry.resolve("guarantee")).get().satisfies(status -> {
            assertThat(status.state()).isEqualTo(ResourceModuleSyncState.UNKNOWN);
            assertThat(status.reasonCode()).isEqualTo("RESOURCE_SYNC_OBSERVATION_FAILED");
        });
    }

    @Test
    void warmSyncFor1964DeclarationsUsesConstantRegistryQueriesAndWritesNoSkipLogs() {
        provider.setDeclarations(declarations(1964));
        syncService.sync();
        long syncLogCount = count("resource_sync_log");
        long changeLogCount = count("resource_change_log");
        handler.resetUpsertCount();
        sqlStatementCounter.reset();

        syncService.sync();

        // The second batched snapshot proves that persisted rows match the just-synchronized declarations.
        assertThat(sqlStatementCounter.count(ResourceRegistryMapper.class, "selectByResourceIds")).isEqualTo(2);
        assertThat(sqlStatementCounter.count(ResourceRegistryMapper.class, "selectByTypeAndBizKeys")).isEqualTo(2);
        assertThat(sqlStatementCounter.count(ResourceRegistryMapper.class, "selectBySourceAndModules")).isEqualTo(1);
        assertThat(sqlStatementCounter.countForMapper(ResourceRegistryMapper.class)).isEqualTo(5);
        assertThat(handler.upsertCount()).isZero();
        assertThat(count("resource_sync_log")).isEqualTo(syncLogCount);
        assertThat(count("resource_change_log")).isEqualTo(changeLogCount);
        assertThat(count("resource_registry")).isEqualTo(1964);
    }

    @Test
    void unchangedBootstrapModuleSkipsBeforeDeclarationParsingAndWritesNothing() {
        ResourceDeclaration declaration = activeDeclaration(1, "提交申请");
        RegisterResourceDeclarationsCommand expand = moduleCommand(
                ResourceApplyMode.EXPAND, "guarantee", List.of(declaration));

        assertThat(syncService.registerDeclarations(expand)).isTrue();
        assertThat(handler.upsertCount()).isEqualTo(1);
        long registryCount = count("resource_registry");
        long syncLogCount = count("resource_sync_log");
        long changeLogCount = count("resource_change_log");
        handler.resetUpsertCount();
        sqlStatementCounter.reset();
        expand.setGeneration(9L);
        expand.getModuleManifests().get(0).setDeclarations("not-json");

        assertThat(syncService.registerDeclarations(expand)).isTrue();

        assertThat(handler.upsertCount()).isZero();
        assertThat(sqlStatementCounter.countForMapper(ResourceRegistryMapper.class)).isZero();
        assertThat(count("resource_registry")).isEqualTo(registryCount);
        assertThat(count("resource_sync_log")).isEqualTo(syncLogCount);
        assertThat(count("resource_change_log")).isEqualTo(changeLogCount);
    }

    @Test
    void moduleReceiptAdvancesOnlyAfterSuccessfulExpandAndFinalize() {
        ResourceDeclaration declaration = activeDeclaration(1, "提交申请");
        RegisterResourceDeclarationsCommand expand = moduleCommand(
                ResourceApplyMode.EXPAND, "guarantee", List.of(declaration));
        assertThat(syncService.registerDeclarations(expand)).isTrue();
        assertThat(stringValue("resource_module_receipt", "state")).isEqualTo("EXPANDED");

        RegisterResourceDeclarationsCommand finalize = moduleCommand(
                ResourceApplyMode.FINALIZE, "guarantee", List.of(declaration));
        assertThat(syncService.registerDeclarations(finalize)).isTrue();
        assertThat(stringValue("resource_module_receipt", "state")).isEqualTo("FINALIZED");

        rebuildTables();
        ResourceDeclaration unsupported = activeDeclaration(1, "失败资源");
        unsupported.setResourceType("UNSUPPORTED");
        unsupported.setTargetModule("missing-target");
        RegisterResourceDeclarationsCommand failing = moduleCommand(
                ResourceApplyMode.EXPAND, "guarantee", List.of(unsupported));
        assertThatThrownBy(() -> syncService.registerDeclarations(failing))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未找到资源处理器");
        assertThat(count("resource_module_receipt")).isZero();
    }

    @Test
    void moduleManifestsFollowDependenciesAndRejectMissingOrCyclicDependencies() {
        ResourceDeclaration binding = genericDeclaration("1900000000000000101",
                "TEST_BINDING", "binding.resource", "test-binding");
        binding.setModuleCode("binding");
        ResourceDeclaration identity = genericDeclaration("1900000000000000102",
                "TEST_USER", "identity.resource", "test-user");
        identity.setModuleCode("identity");
        RegisterResourceDeclarationsCommand ordered = moduleCommand(ResourceApplyMode.EXPAND,
                moduleManifest("binding", List.of("identity"), List.of(binding)),
                moduleManifest("identity", List.of(), List.of(identity)));

        assertThat(syncService.registerDeclarations(ordered)).isTrue();

        assertThat(syncOrderRecorder.resourceTypes()).containsExactly("TEST_USER", "TEST_BINDING");
        assertThat(count("resource_module_receipt")).isEqualTo(2);

        rebuildTables();
        RegisterResourceDeclarationsCommand missing = moduleCommand(ResourceApplyMode.EXPAND,
                moduleManifest("binding", List.of("identity"), List.of(binding)));
        assertThatThrownBy(() -> syncService.registerDeclarations(missing))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Resource 模块依赖缺失: binding -> identity");
        assertThat(count("resource_module_receipt")).isZero();

        RegisterResourceDeclarationsCommand cyclic = moduleCommand(ResourceApplyMode.EXPAND,
                moduleManifest("binding", List.of("identity"), List.of(binding)),
                moduleManifest("identity", List.of("binding"), List.of(identity)));
        assertThatThrownBy(() -> syncService.registerDeclarations(cyclic))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Resource 模块依赖存在循环");
        assertThat(count("resource_module_receipt")).isZero();
    }

    @Test
    void moduleManifestRetriesDependencyNotReadyAfterLaterModuleCreatesDependency() {
        ResourceDeclaration dependent = genericDeclaration("1900000000000000103",
                "TEST_DEFERRED", "deferred.resource", "deferred");
        dependent.setModuleCode("a-dependent");
        ResourceDeclaration dependency = genericDeclaration("1900000000000000104",
                "TEST_PROVIDER", "provider.resource", "provider");
        dependency.setModuleCode("z-provider");

        RegisterResourceDeclarationsCommand command = moduleCommand(ResourceApplyMode.EXPAND,
                moduleManifest("a-dependent", List.of(), List.of(dependent)),
                moduleManifest("z-provider", List.of(), List.of(dependency)));

        assertThat(syncService.registerDeclarations(command)).isTrue();
        assertThat(deferredResourceHandler.upsertCalls()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForList(
                "select module_code from resource_module_receipt order by module_code", String.class))
                .containsExactly("a-dependent", "z-provider");
        assertThat(count("resource_registry")).isEqualTo(2);
    }

    @Test
    void singleChangedModuleSkipsUnchangedMalformedEnvelopeAndOnlyWritesChangedModule() {
        ResourceDeclaration guarantee = activeDeclaration(
                "1900000000000000001", 1, "guarantee.apply.submit", "保函提交");
        guarantee.setModuleCode("guarantee");
        ResourceDeclaration workflow = activeDeclaration(
                "1900000000000000002", 1, "workflow.apply.submit", "流程提交");
        workflow.setModuleCode("workflow");
        RegisterResourceDeclarationsCommand initial = moduleCommand(ResourceApplyMode.EXPAND,
                moduleManifest("guarantee", List.of(), List.of(guarantee)),
                moduleManifest("workflow", List.of("guarantee"), List.of(workflow)));
        assertThat(syncService.registerDeclarations(initial)).isTrue();
        long syncLogCount = count("resource_sync_log");
        long changeLogCount = count("resource_change_log");
        handler.resetUpsertCount();
        sqlStatementCounter.reset();

        ResourceDeclaration changedWorkflow = activeDeclaration(
                "1900000000000000002", 2, "workflow.apply.submit", "流程提交新版");
        changedWorkflow.setModuleCode("workflow");
        ResourceModuleManifestCommand unchanged = moduleManifest(
                "guarantee", List.of(), List.of(guarantee));
        unchanged.setDeclarations("not-json");
        RegisterResourceDeclarationsCommand changed = moduleCommand(ResourceApplyMode.EXPAND,
                unchanged,
                moduleManifest("workflow", List.of("guarantee"), List.of(changedWorkflow)));

        assertThat(syncService.registerDeclarations(changed)).isTrue();

        assertThat(handler.upsertCount()).isEqualTo(1);
        assertThat(count("resource_sync_log")).isEqualTo(syncLogCount + 1);
        assertThat(count("resource_change_log")).isEqualTo(changeLogCount + 1);
        assertThat(jdbcTemplate.queryForObject(
                "select title from message_template where id = 91002", String.class))
                .isEqualTo("流程提交新版");
        assertThat(jdbcTemplate.queryForObject(
                "select resource_version from resource_registry where resource_id = ?", Integer.class,
                guarantee.getId())).isEqualTo(1);
    }

    @Test
    void finalizedModuleSkipsBeforeParsingOnRepeatedFinalize() {
        ResourceDeclaration declaration = activeDeclaration(1, "提交申请");
        RegisterResourceDeclarationsCommand finalize = moduleCommand(
                ResourceApplyMode.FINALIZE, "guarantee", List.of(declaration));
        assertThat(syncService.registerDeclarations(finalize)).isTrue();
        long syncLogCount = count("resource_sync_log");
        long changeLogCount = count("resource_change_log");
        handler.resetUpsertCount();
        finalize.getModuleManifests().get(0).setDeclarations("not-json");

        assertThat(syncService.registerDeclarations(finalize)).isTrue();

        assertThat(handler.upsertCount()).isZero();
        assertThat(count("resource_sync_log")).isEqualTo(syncLogCount);
        assertThat(count("resource_change_log")).isEqualTo(changeLogCount);
        assertThat(stringValue("resource_module_receipt", "state")).isEqualTo("FINALIZED");
    }

    @Test
    void finalizedReceiptAlsoSatisfiesLaterExpandWithoutParsingDeclarations() {
        ResourceDeclaration declaration = activeDeclaration(1, "提交申请");
        RegisterResourceDeclarationsCommand finalize = moduleCommand(
                ResourceApplyMode.FINALIZE, "guarantee", List.of(declaration));
        assertThat(syncService.registerDeclarations(finalize)).isTrue();
        long syncLogCount = count("resource_sync_log");
        handler.resetUpsertCount();
        finalize.setApplyMode(ResourceApplyMode.EXPAND);
        finalize.getModuleManifests().get(0).setDeclarations("not-json");

        assertThat(syncService.registerDeclarations(finalize)).isTrue();

        assertThat(handler.upsertCount()).isZero();
        assertThat(count("resource_sync_log")).isEqualTo(syncLogCount);
        assertThat(stringValue("resource_module_receipt", "state")).isEqualTo("FINALIZED");
    }

    @Test
    void rejectsModuleCountHashAndOwnershipMismatchWithoutWritingReceipt() {
        ResourceDeclaration declaration = activeDeclaration(1, "提交申请");
        ResourceModuleManifestCommand wrongCount = moduleManifest(
                "guarantee", List.of(), List.of(declaration));
        wrongCount.setDeclarationCount(2);
        assertThatThrownBy(() -> syncService.registerDeclarations(
                moduleCommand(ResourceApplyMode.EXPAND, wrongCount)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("声明数量不匹配");
        assertThat(count("resource_module_receipt")).isZero();

        ResourceModuleManifestCommand wrongHash = moduleManifest(
                "guarantee", List.of(), List.of(declaration));
        wrongHash.setModuleHash("0".repeat(64));
        assertThatThrownBy(() -> syncService.registerDeclarations(
                moduleCommand(ResourceApplyMode.EXPAND, wrongHash)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Hash 不匹配");
        assertThat(count("resource_module_receipt")).isZero();

        ResourceDeclaration foreign = activeDeclaration(1, "提交申请");
        foreign.setModuleCode("workflow");
        ResourceModuleManifestCommand wrongOwner = moduleManifest(
                "guarantee", List.of(), List.of(foreign));
        assertThatThrownBy(() -> syncService.registerDeclarations(
                moduleCommand(ResourceApplyMode.EXPAND, wrongOwner)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("声明不属于模块");
        assertThat(count("resource_module_receipt")).isZero();
    }

    @Test
    void changedModuleFailurePreservesPreviousSuccessfulReceiptAndData() {
        ResourceDeclaration initial = activeDeclaration(1, "稳定版本");
        RegisterResourceDeclarationsCommand first = moduleCommand(
                ResourceApplyMode.EXPAND, "guarantee", List.of(initial));
        assertThat(syncService.registerDeclarations(first)).isTrue();
        String oldHash = stringValue("resource_module_receipt", "module_hash");

        ResourceDeclaration unsupported = activeDeclaration(2, "失败版本");
        unsupported.setResourceType("UNSUPPORTED");
        unsupported.setTargetModule("missing-target");
        RegisterResourceDeclarationsCommand failing = moduleCommand(
                ResourceApplyMode.EXPAND, "guarantee", List.of(unsupported));
        failing.setGeneration(9L);
        failing.setManifestFingerprint("e".repeat(64));

        assertThatThrownBy(() -> syncService.registerDeclarations(failing))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未找到资源处理器");

        assertThat(stringValue("resource_module_receipt", "module_hash")).isEqualTo(oldHash);
        assertThat(jdbcTemplate.queryForObject(
                "select generation from resource_module_receipt", Long.class)).isEqualTo(8L);
        assertThat(stringValue("message_template", "title")).isEqualTo("稳定版本");
    }

    @Test
    void multiModuleFailureKeepsCompletedPrefixAndRetrySkipsIt() {
        ResourceDeclaration firstDeclaration = genericDeclaration("1900000000000000101",
                "TEST_USER", "identity.resource", "test-user");
        firstDeclaration.setModuleCode("identity");
        ResourceDeclaration failingDeclaration = activeDeclaration(
                "1900000000000000102", 1, "workflow.resource", "失败资源");
        failingDeclaration.setModuleCode("workflow");
        failingDeclaration.setResourceType("UNSUPPORTED");
        failingDeclaration.setTargetModule("missing-target");
        RegisterResourceDeclarationsCommand failing = moduleCommand(ResourceApplyMode.EXPAND,
                moduleManifest("identity", List.of(), List.of(firstDeclaration)),
                moduleManifest("workflow", List.of("identity"), List.of(failingDeclaration)));

        assertThatThrownBy(() -> syncService.registerDeclarations(failing))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未找到资源处理器");
        assertThat(jdbcTemplate.queryForList(
                "select module_code from resource_module_receipt", String.class))
                .containsExactly("identity");
        syncOrderRecorder.clear();

        ResourceDeclaration recovered = activeDeclaration(
                "1900000000000000102", 1, "workflow.resource", "恢复资源");
        recovered.setModuleCode("workflow");
        RegisterResourceDeclarationsCommand retry = moduleCommand(ResourceApplyMode.EXPAND,
                moduleManifest("identity", List.of(), List.of(firstDeclaration)),
                moduleManifest("workflow", List.of("identity"), List.of(recovered)));
        assertThat(syncService.registerDeclarations(retry)).isTrue();

        assertThat(syncOrderRecorder.resourceTypes()).isEmpty();
        assertThat(jdbcTemplate.queryForList(
                "select module_code from resource_module_receipt order by module_code", String.class))
                .containsExactly("identity", "workflow");
        assertThat(jdbcTemplate.queryForObject(
                "select title from message_template where biz_key = 'workflow.resource'", String.class))
                .isEqualTo("恢复资源");
    }

    @Test
    void receiptsAreIsolatedByEnvironmentApplicationAndService() {
        ResourceModuleManifestCommand empty = moduleManifest("guarantee", List.of(), List.of());
        RegisterResourceDeclarationsCommand first = moduleCommand(ResourceApplyMode.EXPAND, empty);
        assertThat(syncService.registerDeclarations(first)).isTrue();

        RegisterResourceDeclarationsCommand otherEnvironment = moduleCommand(
                ResourceApplyMode.EXPAND, moduleManifest("guarantee", List.of(), List.of()));
        otherEnvironment.setEnvironmentKey("other-env");
        assertThat(syncService.registerDeclarations(otherEnvironment)).isTrue();
        RegisterResourceDeclarationsCommand otherApplication = moduleCommand(
                ResourceApplyMode.EXPAND, moduleManifest("guarantee", List.of(), List.of()));
        otherApplication.setAppCode("other-app");
        assertThat(syncService.registerDeclarations(otherApplication)).isTrue();
        RegisterResourceDeclarationsCommand otherService = moduleCommand(
                ResourceApplyMode.EXPAND, moduleManifest("guarantee", List.of(), List.of()));
        otherService.setServiceCode("other-service");
        assertThat(syncService.registerDeclarations(otherService)).isTrue();

        assertThat(count("resource_module_receipt")).isEqualTo(4);
        assertThat(jdbcTemplate.queryForList("""
                select environment_key, app_code, service_code
                  from resource_module_receipt
                 order by environment_key, app_code, service_code
                """)).containsExactlyInAnyOrder(
                Map.of("environment_key", "test", "app_code", "test-app", "service_code", "test-service"),
                Map.of("environment_key", "other-env", "app_code", "test-app", "service_code", "test-service"),
                Map.of("environment_key", "test", "app_code", "other-app", "service_code", "test-service"),
                Map.of("environment_key", "test", "app_code", "test-app", "service_code", "other-service"));
    }

    @Test
    void finalizeRemovalOnlyDisablesAutoResourcesInChangedModule() {
        ResourceDeclaration auto = activeDeclaration(
                "1900000000000000001", 1, "guarantee.auto", "自动资源");
        auto.setModuleCode("guarantee");
        ResourceDeclaration initOnly = activeDeclaration(
                "1900000000000000002", 1, "guarantee.init-only", "初始化资源");
        initOnly.setModuleCode("guarantee");
        initOnly.setSyncMode(ResourceSyncMode.INIT_ONLY);
        ResourceDeclaration manual = activeDeclaration(
                "1900000000000000003", 1, "guarantee.manual", "人工资源");
        manual.setModuleCode("guarantee");
        manual.setSyncMode(ResourceSyncMode.MANUAL);
        ResourceDeclaration workflow = activeDeclaration(
                "1900000000000000004", 1, "workflow.auto", "流程资源");
        workflow.setModuleCode("workflow");
        RegisterResourceDeclarationsCommand initial = moduleCommand(ResourceApplyMode.FINALIZE,
                moduleManifest("guarantee", List.of(), List.of(auto, initOnly, manual)),
                moduleManifest("workflow", List.of(), List.of(workflow)));
        assertThat(syncService.registerDeclarations(initial)).isTrue();

        ResourceModuleManifestCommand unchangedWorkflow = moduleManifest(
                "workflow", List.of(), List.of(workflow));
        unchangedWorkflow.setDeclarations("not-json");
        RegisterResourceDeclarationsCommand removeGuarantee = moduleCommand(ResourceApplyMode.FINALIZE,
                moduleManifest("guarantee", List.of(), List.of()), unchangedWorkflow);

        assertThat(syncService.registerDeclarations(removeGuarantee)).isTrue();

        assertThat(registryStatus(auto.getId())).isEqualTo("REMOVED");
        assertThat(registryStatus(initOnly.getId())).isEqualTo("ACTIVE");
        assertThat(registryStatus(manual.getId())).isEqualTo("ACTIVE");
        assertThat(registryStatus(workflow.getId())).isEqualTo("ACTIVE");
        assertThat(jdbcTemplate.queryForObject(
                "select enabled from message_template where id = 91001", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select enabled from message_template where id = 91002", Integer.class)).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "select enabled from message_template where id = 91003", Integer.class)).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "select enabled from message_template where id = 91004", Integer.class)).isOne();
    }

    @Test
    void unchanged1291DeclarationModuleSkipsWithinPerformanceBudget() {
        List<ResourceDeclaration> declarations = declarations(1291);
        RegisterResourceDeclarationsCommand command = moduleCommand(
                ResourceApplyMode.EXPAND, "guarantee", declarations);
        assertThat(syncService.registerDeclarations(command)).isTrue();
        handler.resetUpsertCount();
        sqlStatementCounter.reset();
        command.getModuleManifests().get(0).setDeclarations("not-json");

        long startedNanos = System.nanoTime();
        assertThat(syncService.registerDeclarations(command)).isTrue();
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);

        assertThat(elapsedMillis).isLessThan(10_000L);
        assertThat(handler.upsertCount()).isZero();
        assertThat(sqlStatementCounter.countForMapper(ResourceRegistryMapper.class)).isZero();
        assertThat(count("resource_registry")).isEqualTo(1291);
    }

    @Test
    void unchangedDeclarationsAcrossStatusesWriteNoSkipLogs() {
        ResourceDeclaration active = activeDeclaration(
                "1900000000000000001", 1, "guarantee.apply.active", "正常资源");
        ResourceDeclaration initOnly = activeDeclaration(
                "1900000000000000002", 1, "guarantee.apply.init-only", "初始化资源");
        initOnly.setSyncMode(ResourceSyncMode.INIT_ONLY);
        ResourceDeclaration disabled = activeDeclaration(
                "1900000000000000003", 1, "guarantee.apply.disabled", "禁用资源");
        disabled.setStatus(ResourceStatus.DISABLED);
        ResourceDeclaration deprecated = activeDeclaration(
                "1900000000000000004", 1, "guarantee.apply.deprecated", "废弃资源");
        deprecated.setStatus(ResourceStatus.DEPRECATED);
        provider.setDeclarations(List.of(active, initOnly, disabled, deprecated));
        syncService.sync();
        jdbcTemplate.update("update resource_registry set sync_mode = 'MANUAL' where resource_id = ?",
                active.getId());
        long syncLogCount = count("resource_sync_log");
        long changeLogCount = count("resource_change_log");

        syncService.sync();

        assertThat(count("resource_sync_log")).isEqualTo(syncLogCount);
        assertThat(count("resource_change_log")).isEqualTo(changeLogCount);
        assertThat(count("resource_registry")).isEqualTo(4);
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
    void portableBaselineDefersRemoteTargetsWithoutCallingDispatcher() {
        resourceRegistryProperties.setBaselineBuildEnabled(true);
        provider.setDeclaration(remoteOnlyDeclaration());

        syncService.sync();

        assertThat(dispatcher.upsertBatchCount()).isZero();
        assertThat(registryMapper.selectByResourceId("1900000000000000002")).isNull();
        assertThat(count("resource_sync_log")).isZero();
        assertThat(count("resource_change_log")).isZero();
    }

    @Test
    void syncPrefersLocalHandlerWhenRemoteDispatcherAlsoSupportsTargetModule() {
        syncService.sync();

        assertThat(stringValue("message_template", "title")).isEqualTo("提交申请");
        assertThat(dispatcher.upsertBatchCount()).isZero();
    }

    @Test
    void syncOrdersActiveDeclarationsByResourceTypeDependencies() {
        ResourceDeclaration binding = genericDeclaration("1900000000000000101",
                "TEST_BINDING", "guarantee.binding", "test-binding");
        ResourceDeclaration user = genericDeclaration("1900000000000000102",
                "TEST_USER", "guarantee.user", "test-user");
        provider.setDeclarations(List.of(binding, user));

        syncService.sync();

        assertThat(syncOrderRecorder.resourceTypes()).containsExactly("TEST_USER", "TEST_BINDING");
        assertThat(registryMapper.selectByResourceId("1900000000000000101")).isNotNull();
        assertThat(registryMapper.selectByResourceId("1900000000000000102")).isNotNull();
    }

    @Test
    void syncDoesNotReplayUnchangedDependentsWhenDependencyChanges() {
        ResourceDeclaration binding = genericDeclaration("1900000000000000101",
                "TEST_BINDING", "guarantee.binding", "test-binding");
        ResourceDeclaration user = genericDeclaration("1900000000000000102",
                "TEST_USER", "guarantee.user", "test-user");
        provider.setDeclarations(List.of(binding, user));
        syncService.sync();
        syncOrderRecorder.clear();

        ResourceDeclaration updatedUser = genericDeclaration("1900000000000000102",
                "TEST_USER", "guarantee.user", "test-user");
        updatedUser.setVersion(2);
        provider.setDeclarations(List.of(binding, updatedUser));
        syncService.sync();

        assertThat(syncOrderRecorder.resourceTypes()).containsExactly("TEST_USER");
    }

    @Test
    void syncFailsWhenResourceTypeDependenciesHaveCycle() {
        ResourceDeclaration cycleA = genericDeclaration("1900000000000000201",
                "CYCLE_A", "guarantee.cycle-a", "cycle-a");
        ResourceDeclaration cycleB = genericDeclaration("1900000000000000202",
                "CYCLE_B", "guarantee.cycle-b", "cycle-b");
        provider.setDeclarations(List.of(cycleA, cycleB));

        assertThatThrownBy(() -> syncService.sync())
                .isInstanceOf(BizException.class)
                .hasMessage("资源类型依赖存在循环: CYCLE_A -> CYCLE_B -> CYCLE_A");

        assertThat(count("resource_registry")).isZero();
        assertThat(syncOrderRecorder.resourceTypes()).isEmpty();
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
    void preservedResultDoesNotAdvanceSourceHashOrLastSyncTime() {
        syncService.sync();
        ResourceRegistryEntity before = registryMapper.selectByResourceId("1900000000000000001");
        handler.preserveNextUpsert();
        provider.setDeclaration(activeDeclaration(2, "后台值优先"));

        syncService.sync();

        ResourceRegistryEntity after = registryMapper.selectByResourceId("1900000000000000001");
        assertThat(after.getSourceHash()).isEqualTo(before.getSourceHash());
        assertThat(after.getLastSyncTime()).isEqualTo(before.getLastSyncTime());
        assertThat(stringValue("message_template", "title")).isEqualTo("提交申请");
        assertThat(jdbcTemplate.queryForObject(
                "select result from resource_sync_log order by id desc limit 1", String.class))
                .isEqualTo("PRESERVED");
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
                .isInstanceOf(BizException.class)
                .hasMessageContaining("资源声明版本不允许回退");

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
    void remoteSyncReportsIncompleteWhenDistributedLockIsHeld() {
        assertThat(locker.tryLock(ResourceRegistryLock.LOCK_NAME, 30)).isTrue();
        try {
            assertThat(syncService.syncRemote(
                    "platform-admin", "service-a", List.of(activeDeclaration(1, "服务A"))))
                    .isFalse();
            assertThat(count("resource_registry")).isZero();
        } finally {
            locker.unlock(ResourceRegistryLock.LOCK_NAME);
        }
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
                .isInstanceOf(BizException.class)
                .hasMessageContaining("缺失资源禁用时未找到处理器")
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

    private ResourceDeclaration genericDeclaration(String id, String resourceType, String bizKey, String targetModule) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(id);
        declaration.setVersion(1);
        declaration.setResourceType(resourceType);
        declaration.setModuleCode("guarantee");
        declaration.setBizKey(bizKey);
        declaration.setName(bizKey);
        declaration.setTargetModule(targetModule);
        declaration.setFields(new LinkedHashMap<>());
        return declaration;
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
        declaration.putField("title", title);
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

    private List<ResourceDeclaration> declarations(int count) {
        List<ResourceDeclaration> declarations = new ArrayList<>(count);
        for (int index = 1; index <= count; index++) {
            declarations.add(activeDeclaration(
                    Long.toString(1_900_000_000_000_000_000L + index),
                    1,
                    "guarantee.apply.performance-" + index,
                    "性能基线-" + index));
        }
        return declarations;
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

    private RegisterResourceDeclarationsCommand moduleCommand(ResourceApplyMode applyMode, String moduleCode,
                                                              List<ResourceDeclaration> declarations) {
        return moduleCommand(applyMode, moduleManifest(moduleCode, List.of(), declarations));
    }

    private ResourceModuleManifestCommand moduleManifest(String moduleCode, List<String> dependencies,
                                                         List<ResourceDeclaration> declarations) {
        ResourceModuleManifestCommand module = new ResourceModuleManifestCommand();
        module.setModuleCode(moduleCode);
        module.setDependencies(dependencies);
        module.setDeclarations(writeJson(declarations));
        module.setDeclarationCount(declarations.size());
        module.setModuleHash(resourceContentHasher.moduleHash(moduleCode, dependencies, declarations));
        return module;
    }

    private RegisterResourceDeclarationsCommand moduleCommand(ResourceApplyMode applyMode,
                                                              ResourceModuleManifestCommand... modules) {
        RegisterResourceDeclarationsCommand command = new RegisterResourceDeclarationsCommand();
        command.setAppCode("test-app");
        command.setServiceCode("test-service");
        command.setEnvironmentKey("test");
        command.setGeneration(8L);
        command.setManifestFingerprint("f".repeat(64));
        command.setFencingToken(13L);
        command.setApplyMode(applyMode);
        command.setModuleCodes(java.util.Arrays.stream(modules)
                .map(ResourceModuleManifestCommand::getModuleCode).toList());
        command.setModuleManifests(List.of(modules));
        return command;
    }

    private String writeJson(Object value) {
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists resource_module_receipt");
        jdbcTemplate.execute("drop table if exists resource_change_log");
        jdbcTemplate.execute("drop table if exists resource_sync_log");
        jdbcTemplate.execute("drop table if exists resource_registry");
        jdbcTemplate.execute("drop table if exists message_template");
        jdbcTemplate.execute("drop table if exists infra_kv_entry");
        jdbcTemplate.execute("""
                create table resource_module_receipt (
                    environment_key varchar(128) not null,
                    app_code varchar(128) not null,
                    service_code varchar(128) not null,
                    module_code varchar(64) not null,
                    module_hash varchar(64) not null,
                    generation bigint not null,
                    manifest_fingerprint varchar(64) not null,
                    state varchar(32) not null,
                    declaration_count int not null,
                    created_at timestamp default current_timestamp,
                    updated_at timestamp default current_timestamp,
                    primary key (environment_key, app_code, service_code, module_code)
                )
                """);
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
                    tenant_id varchar(64),
                    org_id bigint,
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
                    tenant_id varchar(64),
                    org_id bigint,
                    created_by bigint,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp
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
                    tenant_id varchar(64),
                    org_id bigint,
                    created_by bigint,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp
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

    private String registryStatus(String resourceId) {
        return jdbcTemplate.queryForObject(
                "select status from resource_registry where resource_id = ?", String.class, resourceId);
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
    @Import({ResourceRegistryRepository.class, ResourceModuleReceiptRepository.class,
            ResourceRegistryLock.class, ResourceRegistryService.class})
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
        BootstrapGenerationFence bootstrapGenerationFence() {
            return authority -> {
                if ((authority.generation() != 8L && authority.generation() != 9L)
                        || authority.fencingToken() != 13L) {
                    throw new IllegalStateException("unexpected test authority");
                }
            };
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
        ILocker locker(JdbcTemplate jdbcTemplate) {
            return new KvStoreLocker(new JdbcKvStore(jdbcTemplate));
        }

        @Bean
        ILeaseLocker leaseLocker(JdbcTemplate jdbcTemplate) {
            return new KvStoreLeaseLocker(new JdbcKvStore(jdbcTemplate));
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

        @Bean
        SqlStatementCounter sqlStatementCounter() {
            return new SqlStatementCounter();
        }

        @Bean
        ResourceSyncOrderRecorder resourceSyncOrderRecorder() {
            return new ResourceSyncOrderRecorder();
        }

        @Bean
        OrderedTestResourceHandler testUserResourceHandler(ResourceSyncOrderRecorder recorder) {
            return new OrderedTestResourceHandler("TEST_USER", List.of(), recorder);
        }

        @Bean
        OrderedTestResourceHandler testBindingResourceHandler(ResourceSyncOrderRecorder recorder) {
            return new OrderedTestResourceHandler("TEST_BINDING", List.of("TEST_USER"), recorder);
        }

        @Bean
        OrderedTestResourceHandler cycleAResourceHandler(ResourceSyncOrderRecorder recorder) {
            return new OrderedTestResourceHandler("CYCLE_A", List.of("CYCLE_B"), recorder);
        }

        @Bean
        OrderedTestResourceHandler cycleBResourceHandler(ResourceSyncOrderRecorder recorder) {
            return new OrderedTestResourceHandler("CYCLE_B", List.of("CYCLE_A"), recorder);
        }

        @Bean
        DeferredResourceHandler deferredResourceHandler() {
            return new DeferredResourceHandler();
        }

        @Bean
        ProviderResourceHandler providerResourceHandler() {
            return new ProviderResourceHandler();
        }
    }

    static class ResourceSyncOrderRecorder {

        private final List<String> resourceTypes = new ArrayList<>();

        void add(String resourceType) {
            resourceTypes.add(resourceType);
        }

        List<String> resourceTypes() {
            return List.copyOf(resourceTypes);
        }

        void clear() {
            resourceTypes.clear();
        }
    }

    static class DeferredResourceHandler implements ResourceHandler {

        private final AtomicInteger upsertCalls = new AtomicInteger();

        @Override
        public String resourceType() {
            return "TEST_DEFERRED";
        }

        @Override
        public ResourceSyncResult upsert(ResourceDeclaration resource) {
            if (upsertCalls.getAndIncrement() == 0) {
                throw new DependencyNotReadyException("dependency is not ready");
            }
            return ResourceSyncResult.of(94001L, "test_deferred", "ok");
        }

        @Override
        public ResourceSyncResult disable(ResourceDeclaration resource) {
            return ResourceSyncResult.of(94001L, "test_deferred", "disabled");
        }

        int upsertCalls() {
            return upsertCalls.get();
        }
    }

    static class ProviderResourceHandler implements ResourceHandler {

        @Override
        public String resourceType() {
            return "TEST_PROVIDER";
        }

        @Override
        public ResourceSyncResult upsert(ResourceDeclaration resource) {
            return ResourceSyncResult.of(94002L, "test_provider", "ok");
        }

        @Override
        public ResourceSyncResult disable(ResourceDeclaration resource) {
            return ResourceSyncResult.of(94002L, "test_provider", "disabled");
        }
    }

    static class MutableResourceProvider implements ResourceProvider {

        private final AtomicReference<List<ResourceDeclaration>> declarations = new AtomicReference<>(List.of());
        private final AtomicReference<RuntimeException> failure = new AtomicReference<>();

        void setDeclaration(ResourceDeclaration declaration) {
            this.declarations.set(List.of(declaration));
        }

        void setDeclarations(List<ResourceDeclaration> declarations) {
            this.declarations.set(List.copyOf(declarations));
        }

        void clear() {
            this.declarations.set(List.of());
        }

        void failWith(RuntimeException exception) {
            failure.set(exception);
        }

        @Override
        public List<String> moduleCodes() {
            return List.of("guarantee");
        }

        @Override
        public List<ResourceDeclaration> provide() {
            RuntimeException exception = failure.getAndSet(null);
            if (exception != null) {
                throw exception;
            }
            return declarations.get();
        }
    }

    static class OrderedTestResourceHandler implements ResourceHandler {

        private final String resourceType;
        private final List<String> dependencies;
        private final ResourceSyncOrderRecorder recorder;

        OrderedTestResourceHandler(String resourceType, List<String> dependencies, ResourceSyncOrderRecorder recorder) {
            this.resourceType = resourceType;
            this.dependencies = dependencies;
            this.recorder = recorder;
        }

        @Override
        public String resourceType() {
            return resourceType;
        }

        @Override
        public List<String> dependsOnResourceTypes() {
            return dependencies;
        }

        @Override
        public ResourceSyncResult upsert(ResourceDeclaration resource) {
            recorder.add(resourceType);
            return ResourceSyncResult.of(targetId(resource), resourceType.toLowerCase(), "ok");
        }

        @Override
        public ResourceSyncResult disable(ResourceDeclaration resource) {
            return ResourceSyncResult.of(targetId(resource), resourceType.toLowerCase(), "disabled");
        }

        @Override
        public ResourceSyncResult delete(ResourceDeclaration resource) {
            return ResourceSyncResult.of(targetId(resource), resourceType.toLowerCase(), "deleted");
        }

        private Long targetId(ResourceDeclaration resource) {
            String id = resource.getId();
            return 93000L + Long.parseLong(id.substring(id.length() - 4));
        }
    }

    static class TestMessageResourceHandler implements ResourceHandler {

        private final TestMessageTemplateMapper messageTemplateMapper;
        private final AtomicInteger upsertCount = new AtomicInteger();
        private final AtomicBoolean preserveNext = new AtomicBoolean();
        private final AtomicBoolean environmentRequired = new AtomicBoolean();
        private final AtomicReference<CountDownLatch> enteredUpsert = new AtomicReference<>();
        private final AtomicReference<CountDownLatch> releaseUpsert = new AtomicReference<>();

        TestMessageResourceHandler(TestMessageTemplateMapper messageTemplateMapper) {
            this.messageTemplateMapper = messageTemplateMapper;
        }

        @Override
        public String resourceType() {
            return "MESSAGE_TEMPLATE";
        }

        @Override
        public ResourceBaselinePolicy baselinePolicy() {
            return environmentRequired.get()
                    ? ResourceBaselinePolicy.ENVIRONMENT_REQUIRED
                    : ResourceBaselinePolicy.PORTABLE;
        }

        @Override
        public ResourceSyncResult upsert(ResourceDeclaration resource) {
            upsertCount.incrementAndGet();
            awaitReleaseIfBlocked();
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
        public Map<String, ResourceSyncResult> upsertBatchWithContext(
                List<ResourceDeclaration> declarations,
                List<ResourceDeclaration> completeBatch,
                Map<String, ResourceSyncContext> syncContexts) {
            if (!preserveNext.getAndSet(false)) {
                return ResourceHandler.super.upsertBatchWithContext(
                        declarations, completeBatch, syncContexts);
            }
            Map<String, ResourceSyncResult> results = new LinkedHashMap<>();
            declarations.forEach(declaration -> results.put(
                    declaration.getId(),
                    ResourceSyncResult.preserved(targetId(declaration), "message_template", "preserved")));
            return results;
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

        void blockNextUpsert() {
            enteredUpsert.set(new CountDownLatch(1));
            releaseUpsert.set(new CountDownLatch(1));
        }

        boolean awaitBlockedUpsert(long timeout, TimeUnit unit) throws InterruptedException {
            CountDownLatch latch = enteredUpsert.get();
            return latch != null && latch.await(timeout, unit);
        }

        void releaseBlockedUpsert() {
            CountDownLatch latch = releaseUpsert.get();
            if (latch != null) {
                latch.countDown();
            }
        }

        void resetBlocking() {
            releaseBlockedUpsert();
            enteredUpsert.set(null);
            releaseUpsert.set(null);
        }

        int upsertCount() {
            return upsertCount.get();
        }

        void resetUpsertCount() {
            upsertCount.set(0);
        }

        void preserveNextUpsert() {
            preserveNext.set(true);
        }

        void resetPreservation() {
            preserveNext.set(false);
        }

        void setEnvironmentRequired(boolean value) {
            environmentRequired.set(value);
        }

        private void awaitReleaseIfBlocked() {
            CountDownLatch entered = enteredUpsert.get();
            CountDownLatch release = releaseUpsert.get();
            if (entered == null || release == null) {
                return;
            }
            entered.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to release blocked test upsert");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Blocked test upsert was interrupted", exception);
            }
        }
    }

    @Intercepts(@Signature(
            type = Executor.class,
            method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}))
    static class SqlStatementCounter implements Interceptor {

        private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();
        private final AtomicReference<StatementFailure> failure = new AtomicReference<>();

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
            int invocationCount = counts.computeIfAbsent(
                    statement.getId(), ignored -> new AtomicInteger()).incrementAndGet();
            StatementFailure configuredFailure = failure.get();
            if (configuredFailure != null
                    && configuredFailure.statementId().equals(statement.getId())
                    && configuredFailure.invocation() == invocationCount) {
                failure.compareAndSet(configuredFailure, null);
                throw new IllegalStateException("simulated diagnostic snapshot failure");
            }
            return invocation.proceed();
        }

        void failOnInvocation(Class<?> mapperType, String statementName, int invocation) {
            failure.set(new StatementFailure(mapperType.getName() + "." + statementName, invocation));
        }

        int count(Class<?> mapperType, String statementName) {
            AtomicInteger count = counts.get(mapperType.getName() + "." + statementName);
            return count == null ? 0 : count.get();
        }

        int countForMapper(Class<?> mapperType) {
            String prefix = mapperType.getName() + ".";
            return counts.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(prefix))
                    .mapToInt(entry -> entry.getValue().get())
                    .sum();
        }

        void reset() {
            counts.clear();
            failure.set(null);
        }

        private record StatementFailure(String statementId, int invocation) {
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
