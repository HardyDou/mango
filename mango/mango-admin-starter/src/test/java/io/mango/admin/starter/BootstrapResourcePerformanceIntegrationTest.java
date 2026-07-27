package io.mango.admin.starter;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.domain.api.DomainApi;
import io.mango.domain.api.command.CreateDomainCommand;
import io.mango.domain.api.command.UpdateDomainCommand;
import io.mango.domain.api.command.UpdateDomainStatusCommand;
import io.mango.domain.api.query.DomainPageQuery;
import io.mango.domain.api.vo.DomainVO;
import io.mango.file.core.config.FileProperties;
import io.mango.file.core.resource.FileAssetResourceHandler;
import io.mango.file.core.resource.FileStorageConfigResourceHandler;
import io.mango.file.core.storage.FileStorageRouter;
import io.mango.file.core.storage.LocalFileStorage;
import io.mango.infra.bootstrap.api.BootstrapAction;
import io.mango.infra.bootstrap.api.BootstrapPhase;
import io.mango.infra.bootstrap.api.BootstrapStep;
import io.mango.infra.bootstrap.api.BootstrapStepContributor;
import io.mango.infra.bootstrap.api.BootstrapStepResult;
import io.mango.infra.bootstrap.api.BootstrapStrategy;
import io.mango.infra.bootstrap.core.BootstrapDatabaseLock;
import io.mango.infra.bootstrap.core.BootstrapManifestHasher;
import io.mango.infra.bootstrap.core.BootstrapOrchestrator;
import io.mango.infra.bootstrap.core.BootstrapOutcome;
import io.mango.infra.bootstrap.core.BootstrapPlanBuilder;
import io.mango.infra.bootstrap.core.BootstrapRequest;
import io.mango.infra.bootstrap.core.BootstrapSchemaMigrator;
import io.mango.infra.bootstrap.core.JdbcBootstrapRepository;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.kv.api.ILeaseLocker;
import io.mango.infra.kv.core.capability.KvStoreLeaseLocker;
import io.mango.infra.kv.core.jdbc.JdbcKvStore;
import io.mango.infra.persistence.starter.PersistenceAuditAutoConfiguration;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.core.diagnostic.ResourceModuleSyncStatusRegistry;
import io.mango.resource.core.service.impl.ResourceRegistryService;
import io.mango.resource.core.sync.ResourceContentHasher;
import io.mango.resource.core.sync.ResourceRegistryLock;
import io.mango.resource.core.sync.ResourceRegistryRepository;
import io.mango.resource.api.ResourceDeclarationApi;
import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.builder.ResourceDeclarationBuilder;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.sync.starter.ResourceBootstrapStepContributor;
import io.mango.workflow.core.engine.WorkflowAssigneeResolver;
import io.mango.workflow.core.engine.WorkflowDesignerBpmnConverter;
import io.mango.workflow.core.resource.WorkflowCategoryResourceHandler;
import io.mango.workflow.core.resource.WorkflowDefinitionResourceHandler;
import io.mango.workflow.core.service.impl.WorkflowDefinitionService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("performance")
@EnabledIfEnvironmentVariable(named = "MANGO_BOOTSTRAP_RESOURCE_PERF_DB_URL", matches = "jdbc:mysql:.+")
class BootstrapResourcePerformanceIntegrationTest {

    private static final int SCALE_FACTOR = 5;
    private static final int BAOHAN_DECLARATION_COUNT = 232;
    private static final int BAOHAN_WORKFLOW_COUNT = 4;
    private static final int BAOHAN_FILE_COUNT = 15;
    private static final int WORKFLOW_COUNT = BAOHAN_WORKFLOW_COUNT * SCALE_FACTOR;
    private static final int FILE_COUNT = BAOHAN_FILE_COUNT * SCALE_FACTOR;
    private static final int RESOURCE_COUNT =
            (BAOHAN_DECLARATION_COUNT + BAOHAN_WORKFLOW_COUNT + BAOHAN_FILE_COUNT)
                    * SCALE_FACTOR;
    private static final int FILE_SIZE = 1024 * 1024;
    private static final int CATEGORY_COUNT = RESOURCE_COUNT - WORKFLOW_COUNT - FILE_COUNT - 1;
    private static final long COLD_LIMIT_MILLIS = Duration.ofSeconds(60).toMillis();
    private static final long WARM_LIMIT_MILLIS = Duration.ofSeconds(10).toMillis();
    private static final String DATABASE_SUFFIX = "_bootstrap_resource_perf";
    private static final Long STORAGE_CONFIG_ID = 8_400_000_000_000_000_001L;

    @Test
    void injectsFiveTimesBaohanResourceVolumeThroughRealWorkflowAndFileHandlers() throws Exception {
        String jdbcUrl = requiredEnvironment("MANGO_BOOTSTRAP_RESOURCE_PERF_DB_URL");
        String username = environment("MANGO_BOOTSTRAP_RESOURCE_PERF_DB_USERNAME", "root");
        String password = environment("MANGO_BOOTSTRAP_RESOURCE_PERF_DB_PASSWORD", "");
        assertDedicatedDatabase(jdbcUrl);

        DriverManagerDataSource dataSource = new DriverManagerDataSource(jdbcUrl, username, password);
        assertEmptySchema(dataSource);

        Path classpathRoot = Files.createTempDirectory("mango-bootstrap-resource-classpath-");
        Path storageRoot = Files.createTempDirectory("mango-bootstrap-resource-storage-");
        URLClassLoader assetClassLoader = new URLClassLoader(
                new URL[]{classpathRoot.toUri().toURL()}, Thread.currentThread().getContextClassLoader());
        ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(assetClassLoader);

        try {
            GeneratedAssets assets = generateAssets(classpathRoot);
            long schemaStarted = System.nanoTime();
            migrateSchemas(dataSource);
            long schemaElapsed = elapsedMillis(schemaStarted);

            try (AnnotationConfigApplicationContext context = applicationContext(
                    dataSource, storageRoot, assetClassLoader)) {
                PerformanceResourceProvider provider = context.getBean(PerformanceResourceProvider.class);
                ResourceRegistryService registryService = context.getBean(ResourceRegistryService.class);
                ResourceRegistryProperties registryProperties = context.getBean(ResourceRegistryProperties.class);
                ResourceDeclarationCollector collector = context.getBean(ResourceDeclarationCollector.class);
                ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
                JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
                RepositoryService repositoryService = context.getBean(RepositoryService.class);
                List<ResourceDeclaration> foundationDeclarations = foundationDeclarations();
                List<ResourceDeclaration> fileDeclarations = fileDeclarations(assets);
                List<ResourceDeclaration> workflowDeclarations = workflowDeclarations();

                MangoContextHolder.set(MangoContextSnapshot.empty()
                        .withSecurity(1L, "1", "bootstrap-perf", "INTERNAL",
                                "INTERNAL_USER", "INTERNAL_ORG", 1L, "bootstrap-perf"));

                provider.setDeclarations(concat(
                        foundationDeclarations, fileDeclarations, workflowDeclarations));
                BootstrapOrchestrator orchestrator = bootstrapOrchestrator(
                        dataSource, context.getBean(JdbcBootstrapRepository.class),
                        registryProperties, collector, registryService, objectMapper);
                BootstrapRequest applyRequest = request(BootstrapAction.APPLY);
                long coldStarted = System.nanoTime();
                BootstrapOutcome coldOutcome = orchestrator.execute(applyRequest);
                long coldElapsed = elapsedMillis(coldStarted);

                assertThat(coldOutcome.state()).isEqualTo("FINALIZED");
                assertThat(coldOutcome.executedSteps()).isEqualTo(3);
                assertThat(count(jdbcTemplate, "resource_registry")).isEqualTo(RESOURCE_COUNT);
                assertThat(count(jdbcTemplate, "workflow_category")).isEqualTo(CATEGORY_COUNT + WORKFLOW_COUNT);
                assertThat(count(jdbcTemplate, "workflow_definition")).isEqualTo(WORKFLOW_COUNT);
                assertThat(count(jdbcTemplate, "workflow_definition_version")).isEqualTo(WORKFLOW_COUNT);
                assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(WORKFLOW_COUNT);
                assertThat(count(jdbcTemplate, "file_record")).isEqualTo(FILE_COUNT);
                assertThat(count(jdbcTemplate, "file_object")).isEqualTo(FILE_COUNT);
                verifyStoredAssets(storageRoot, assets);

                long syncLogCount = count(jdbcTemplate, "resource_sync_log");
                long warmStarted = System.nanoTime();
                BootstrapOutcome warmOutcome = orchestrator.execute(applyRequest);
                long warmElapsed = elapsedMillis(warmStarted);

                assertThat(warmOutcome.state()).isEqualTo("FINALIZED");
                assertThat(warmOutcome.executedSteps()).isZero();
                assertThat(orchestrator.execute(request(BootstrapAction.VERIFY)).state()).isEqualTo("VERIFIED");
                assertThat(count(jdbcTemplate, "resource_registry")).isEqualTo(RESOURCE_COUNT);
                assertThat(count(jdbcTemplate, "resource_sync_log")).isEqualTo(syncLogCount);
                assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(WORKFLOW_COUNT);
                verifyStoredAssets(storageRoot, assets);
                assertThat(count(jdbcTemplate, "mango_bootstrap_control")).isEqualTo(1);

                System.out.printf("%nMANGO_BOOTSTRAP_RESOURCE_PERF%n"
                                + "scaleFactor=%d%nbaohanDeclarations=%d%nbaohanWorkflowDefinitions=%d%n"
                                + "baohanFileAssets=%d%ndeclarations=%d%nworkflowDefinitions=%d%n"
                                + "workflowNodesPerDefinition=%d%n"
                                + "fileAssets=%d%nfileBytes=%d%nschemaSetupMs=%d%n"
                                + "bootstrapColdMs=%d%nbootstrapWarmMs=%d%n",
                        SCALE_FACTOR, BAOHAN_DECLARATION_COUNT, BAOHAN_WORKFLOW_COUNT,
                        BAOHAN_FILE_COUNT, RESOURCE_COUNT, WORKFLOW_COUNT, 8,
                        FILE_COUNT, assets.totalBytes(),
                        schemaElapsed, coldElapsed, warmElapsed);

                assertThat(coldElapsed)
                        .as("cold Resource injection must complete within 60 seconds")
                        .isLessThan(COLD_LIMIT_MILLIS);
                assertThat(warmElapsed)
                        .as("unchanged Resource verification must complete within 10 seconds")
                        .isLessThan(WARM_LIMIT_MILLIS);
            } finally {
                MangoContextHolder.clear();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
            assetClassLoader.close();
            deleteRecursively(classpathRoot);
            deleteRecursively(storageRoot);
        }
    }

    private BootstrapOrchestrator bootstrapOrchestrator(
            DataSource dataSource,
            JdbcBootstrapRepository repository,
            ResourceRegistryProperties properties,
            ResourceDeclarationCollector collector,
            ResourceRegistryService registryService,
            ObjectMapper objectMapper) {
        BootstrapManifestHasher hasher = new BootstrapManifestHasher();
        ResourceDeclarationApi declarationApi = command -> R.ok(registryService.registerDeclarations(command));
        ResourceBootstrapStepContributor resourceContributor = new ResourceBootstrapStepContributor(
                properties, collector, declarationApi, objectMapper, "bootstrap-performance");
        List<BootstrapStepContributor> contributors = List.of(
                () -> List.of(noOpFlywayExpandStep()), resourceContributor);
        return new BootstrapOrchestrator(
                new BootstrapPlanBuilder(hasher), hasher, new BootstrapSchemaMigrator(dataSource),
                new BootstrapDatabaseLock(dataSource), repository, contributors);
    }

    private BootstrapStep noOpFlywayExpandStep() {
        return new BootstrapStep() {
            @Override
            public String code() {
                return "FLYWAY_EXPAND";
            }

            @Override
            public BootstrapPhase phase() {
                return BootstrapPhase.EXPAND;
            }

            @Override
            public Set<String> dependencies() {
                return Set.of();
            }

            @Override
            public String fingerprintMaterial() {
                return "performance-schema-v1";
            }

            @Override
            public BootstrapStepResult execute(io.mango.infra.bootstrap.api.BootstrapExecutionContext context) {
                return BootstrapStepResult.completed("Performance schemas already prepared");
            }
        };
    }

    private BootstrapRequest request(BootstrapAction action) {
        return new BootstrapRequest(
                "bootstrap-resource-performance", "performance-release", "performance-revision",
                1L, null, action, BootstrapStrategy.COLD, null, 30);
    }

    private AnnotationConfigApplicationContext applicationContext(DataSource dataSource, Path storageRoot,
                                                                   ClassLoader assetClassLoader) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getSystemProperties().put(
                "mango.persistence.mybatis-plus.tenant.enabled", "false");
        context.getBeanFactory().registerSingleton("dataSource", dataSource);
        context.getBeanFactory().registerSingleton("performanceStorageRoot", storageRoot);
        context.getBeanFactory().registerSingleton("performanceAssetClassLoader", assetClassLoader);
        context.register(TestConfig.class);
        context.refresh();
        return context;
    }

    private void migrateSchemas(DataSource dataSource) {
        migrate(dataSource, "flyway_schema_history_resource_perf", "classpath:db/migration/resource");
        migrate(dataSource, "flyway_schema_history_kv_perf", "classpath:db/migration/kv");
        migrate(dataSource, "flyway_schema_history_file_perf", "classpath:db/migration/file");
        migrate(dataSource, "flyway_schema_history_workflow_perf", "classpath:db/migration/workflow");
    }

    private void migrate(DataSource dataSource, String historyTable, String location) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations(location)
                .table(historyTable)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .outOfOrder(true)
                .load()
                .migrate();
    }

    private List<ResourceDeclaration> foundationDeclarations() {
        List<ResourceDeclaration> declarations = new ArrayList<>(CATEGORY_COUNT + 1);
        for (int index = 0; index < CATEGORY_COUNT; index++) {
            declarations.add(categoryDeclaration(index));
        }
        declarations.add(storageConfigDeclaration());
        return List.copyOf(declarations);
    }

    private List<ResourceDeclaration> fileDeclarations(GeneratedAssets assets) {
        List<ResourceDeclaration> declarations = new ArrayList<>(FILE_COUNT);
        for (GeneratedAsset asset : assets.assets()) {
            declarations.add(fileDeclaration(asset));
        }
        return List.copyOf(declarations);
    }

    private List<ResourceDeclaration> workflowDeclarations() {
        List<ResourceDeclaration> declarations = new ArrayList<>(WORKFLOW_COUNT);
        for (int index = 0; index < WORKFLOW_COUNT; index++) {
            declarations.add(workflowDeclaration(index));
        }
        return List.copyOf(declarations);
    }

    @SafeVarargs
    private final List<ResourceDeclaration> concat(List<ResourceDeclaration>... groups) {
        List<ResourceDeclaration> declarations = new ArrayList<>(RESOURCE_COUNT);
        for (List<ResourceDeclaration> group : groups) {
            declarations.addAll(group);
        }
        return List.copyOf(declarations);
    }

    private ResourceDeclaration categoryDeclaration(int index) {
        return base(ResourceTypes.WORKFLOW_CATEGORY,
                8_100_000_000_000_000_000L + index,
                "bootstrap.perf.workflow-category." + index,
                "性能流程分类 " + index,
                "workflow")
                .longValue("categoryId", 8_200_000_000_000_000_000L + index)
                .longValue("tenantId", 1L)
                .string("categoryCode", "PERF_CATEGORY_" + index)
                .string("categoryName", "性能流程分类 " + index)
                .string("domainCode", "PERF")
                .intValue("sort", index)
                .intValue("status", 1)
                .string("remark", "Bootstrap Resource 性能基准")
                .build();
    }

    private ResourceDeclaration storageConfigDeclaration() {
        return base(ResourceTypes.FILE_STORAGE_CONFIG,
                8_300_000_000_000_000_001L,
                "bootstrap.perf.file-storage",
                "性能基准本地存储",
                "file")
                .longValue("storageConfigId", STORAGE_CONFIG_ID)
                .longValue("tenantId", 1L)
                .string("configName", "bootstrap-resource-performance-local")
                .string("storageType", "LOCAL")
                .string("bucketName", "bootstrap-perf")
                .intValue("active", 1)
                .intValue("status", 1)
                .build();
    }

    private ResourceDeclaration fileDeclaration(GeneratedAsset asset) {
        return base(ResourceTypes.FILE_ASSET,
                8_300_000_000_000_001_000L + asset.index(),
                "bootstrap.perf.file." + asset.index(),
                "性能附件 " + asset.index(),
                "file")
                .longValue("tenantId", 1L)
                .longValue("fileId", 8_300_000_000_000_002_000L + asset.index())
                .longValue("storageConfigId", STORAGE_CONFIG_ID)
                .string("objectName", asset.objectName())
                .string("fileName", "performance-asset-" + asset.index() + ".bin")
                .string("sha256", asset.sha256())
                .file("content", asset.classpathLocation(), null, "application/octet-stream")
                .string("purpose", "workflow-attachment-template")
                .string("bizType", "WORKFLOW_DEFINITION")
                .string("bizId", "PERF_WORKFLOW_" + (asset.index() % WORKFLOW_COUNT))
                .build();
    }

    private ResourceDeclaration workflowDeclaration(int index) {
        return base(ResourceTypes.WORKFLOW_DEFINITION,
                8_500_000_000_000_000_000L + index,
                "bootstrap.perf.workflow-definition." + index,
                "复杂审批流程 " + index,
                "workflow")
                .longValue("tenantId", 1L)
                .string("domainCode", "PERF")
                .string("categoryCode", "PERF_DEFINITION_CATEGORY_" + index)
                .string("categoryName", "性能流程定义分类 " + index)
                .intValue("categorySort", 2_000 + index)
                .longValue("orgId", 1L)
                .list("adminUsers", List.of("admin", "risk-manager"))
                .bool("startEntryVisible", false)
                .string("definitionKey", "PERF_COMPLEX_APPROVAL_" + index)
                .string("definitionName", "复杂审批流程 " + index)
                .json("designerJson", designerGraph(index))
                .string("formCode", "perf_complex_approval_" + index)
                .json("formJson", formDefinition(index))
                .string("remark", "八级审批与完整动态表单的 Bootstrap 性能基准")
                .build();
    }

    private ResourceDeclarationBuilder base(String type, long id, String bizKey, String name,
                                            String targetModule) {
        return ResourceDeclarationBuilder.create(type)
                .id(Long.toString(id))
                .version(1)
                .module("bootstrap-performance", "Bootstrap 性能基准")
                .bizKey(bizKey)
                .name(name)
                .targetModule(targetModule);
    }

    private Map<String, Object> designerGraph(int workflowIndex) {
        Map<String, Object> child = null;
        for (int level = 8; level >= 1; level--) {
            child = Map.of(
                    "id", "workflow_" + workflowIndex + "_approval_" + level,
                    "nodeType", "APPROVAL",
                    "nodeName", "第 " + level + " 级审批",
                    "properties", Map.of(
                            "approvalConfig", Map.of(
                                    "assigneeType", "SPECIFIED_USER",
                                    "assigneeIds", List.of("approver-" + level),
                                    "approvalMode", "OR_SIGN",
                                    "emptyAssigneeStrategy", "TO_ADMIN"),
                            "formPermissions", Map.of(
                                    "applicant", Map.of("readable", true, "editable", false),
                                    "amount", Map.of("readable", true, "editable", level == 1))),
                    "childNode", child == null ? Map.of() : child);
        }
        return Map.of(
                "id", "workflow_" + workflowIndex + "_root",
                "nodeType", "ROOT",
                "nodeName", "发起申请",
                "childNode", child);
    }

    private Map<String, Object> formDefinition(int workflowIndex) {
        List<Map<String, Object>> fields = new ArrayList<>();
        for (int index = 0; index < 24; index++) {
            fields.add(Map.of(
                    "key", "field_" + index,
                    "label", "业务字段 " + index,
                    "type", index % 3 == 0 ? "number" : "input",
                    "required", index < 8,
                    "defaultValue", "workflow-" + workflowIndex + "-value-" + index));
        }
        return Map.of(
                "schemaVersion", 1,
                "layout", Map.of("columns", 2, "labelWidth", 120),
                "fields", fields);
    }

    private GeneratedAssets generateAssets(Path classpathRoot) throws IOException, NoSuchAlgorithmException {
        Path assetDirectory = classpathRoot.resolve("META-INF/mango/assets/bootstrap-performance");
        Files.createDirectories(assetDirectory);
        List<GeneratedAsset> assets = new ArrayList<>();
        long totalBytes = 0;
        for (int index = 0; index < FILE_COUNT; index++) {
            byte[] content = generatedContent(index);
            String fileName = "performance-asset-" + index + ".bin";
            Files.write(assetDirectory.resolve(fileName), content);
            String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
            assets.add(new GeneratedAsset(
                    index,
                    "classpath:META-INF/mango/assets/bootstrap-performance/" + fileName,
                    "mango-assets/bootstrap-performance/" + fileName,
                    sha256,
                    content.length));
            totalBytes += content.length;
        }
        return new GeneratedAssets(List.copyOf(assets), totalBytes);
    }

    private byte[] generatedContent(int assetIndex) {
        byte[] content = new byte[FILE_SIZE];
        byte[] marker = ("mango-bootstrap-resource-performance-" + assetIndex + "\n")
                .getBytes(StandardCharsets.UTF_8);
        for (int offset = 0; offset < content.length; offset += marker.length) {
            System.arraycopy(marker, 0, content, offset, Math.min(marker.length, content.length - offset));
        }
        return content;
    }

    private void verifyStoredAssets(Path storageRoot, GeneratedAssets assets) throws Exception {
        for (GeneratedAsset asset : assets.assets()) {
            Path stored = storageRoot.resolve("bootstrap-perf").resolve(asset.objectName());
            assertThat(stored).isRegularFile();
            assertThat(Files.size(stored)).isEqualTo(asset.size());
            try (var input = Files.newInputStream(stored)) {
                assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(input.readAllBytes()))).isEqualTo(asset.sha256());
            }
        }
    }

    private void assertDedicatedDatabase(String jdbcUrl) {
        String withoutParameters = jdbcUrl.substring(0, jdbcUrl.indexOf('?') >= 0
                ? jdbcUrl.indexOf('?') : jdbcUrl.length());
        String database = withoutParameters.substring(withoutParameters.lastIndexOf('/') + 1);
        assertThat(database)
                .as("MANGO_BOOTSTRAP_RESOURCE_PERF_DB_URL must reference a dedicated performance database")
                .endsWith(DATABASE_SUFFIX);
    }

    private void assertEmptySchema(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Long tableCount = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = database()
                """, Long.class);
        assertThat(tableCount)
                .as("MANGO_BOOTSTRAP_RESOURCE_PERF_DB_URL must reference an empty database")
                .isZero();
    }

    private long count(JdbcTemplate jdbcTemplate, String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    private long elapsedMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        assertThat(value).as(name + " must be set").isNotBlank();
        return value;
    }

    private String environment(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null ? defaultValue : value;
    }

    private void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private record GeneratedAsset(int index, String classpathLocation, String objectName,
                                  String sha256, long size) {
    }

    private record GeneratedAssets(List<GeneratedAsset> assets, long totalBytes) {
    }

    static final class PerformanceResourceProvider implements ResourceProvider {

        private List<ResourceDeclaration> declarations = List.of();

        void setDeclarations(List<ResourceDeclaration> declarations) {
            this.declarations = List.copyOf(declarations);
        }

        @Override
        public List<String> moduleCodes() {
            return List.of("bootstrap-performance");
        }

        @Override
        public List<ResourceDeclaration> provide() {
            return declarations;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @MapperScan(basePackages = {
            "io.mango.resource.core.mapper",
            "io.mango.file.core.mapper",
            "io.mango.workflow.core.mapper"
    })
    @Import({
            MybatisPlusAutoConfiguration.class,
            PersistenceMybatisPlusAutoConfiguration.class,
            PersistenceAuditAutoConfiguration.class,
            ResourceRegistryRepository.class,
            ResourceRegistryLock.class,
            ResourceRegistryService.class,
            FileStorageConfigResourceHandler.class,
            FileAssetResourceHandler.class,
            WorkflowCategoryResourceHandler.class,
            WorkflowDefinitionResourceHandler.class,
            WorkflowDefinitionService.class,
            WorkflowAssigneeResolver.class,
            WorkflowDesignerBpmnConverter.class
    })
    static class TestConfig {

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        JdbcBootstrapRepository jdbcBootstrapRepository(DataSource dataSource) {
            return new JdbcBootstrapRepository(new JdbcTemplate(dataSource));
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        ResourceRegistryProperties resourceRegistryProperties() {
            ResourceRegistryProperties properties = new ResourceRegistryProperties();
            properties.setLocations(List.of());
            properties.setInstanceId("bootstrap-resource-performance");
            properties.setLockTtlSeconds(120);
            return properties;
        }

        @Bean
        ResourceDeclarationCollector resourceDeclarationCollector(ObjectProvider<ResourceProvider> providers) {
            return new ResourceDeclarationCollector(providers);
        }

        @Bean
        ResourceContentHasher resourceContentHasher(ObjectMapper objectMapper) {
            return new ResourceContentHasher(objectMapper);
        }

        @Bean
        ResourceModuleSyncStatusRegistry resourceModuleSyncStatusRegistry(ResourceContentHasher hasher) {
            return new ResourceModuleSyncStatusRegistry(hasher);
        }

        @Bean
        ILeaseLocker leaseLocker(JdbcTemplate jdbcTemplate) {
            return new KvStoreLeaseLocker(new JdbcKvStore(jdbcTemplate));
        }

        @Bean
        PerformanceResourceProvider performanceResourceProvider() {
            return new PerformanceResourceProvider();
        }

        @Bean
        ResourceLoader resourceLoader(ClassLoader performanceAssetClassLoader) {
            return new DefaultResourceLoader(performanceAssetClassLoader);
        }

        @Bean
        FileProperties fileProperties(Path performanceStorageRoot) {
            FileProperties properties = new FileProperties();
            properties.getLocal().setRootPath(performanceStorageRoot.toString());
            return properties;
        }

        @Bean
        LocalFileStorage localFileStorage(FileProperties properties) {
            return new LocalFileStorage(properties);
        }

        @Bean
        FileStorageRouter fileStorageRouter(LocalFileStorage localFileStorage) {
            return new FileStorageRouter(List.of(localFileStorage));
        }

        @Bean(destroyMethod = "close")
        ProcessEngine processEngine(DataSource dataSource, JdbcTemplate jdbcTemplate) {
            initializeFlowableMetadata(jdbcTemplate);
            ProcessEngineConfigurationImpl configuration =
                    (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
                            .createStandaloneProcessEngineConfiguration();
            configuration.setDisableIdmEngine(true);
            configuration.setDisableEventRegistry(true);
            return configuration
                    .setDataSource(dataSource)
                    .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                    .setAsyncExecutorActivate(false)
                    .setAsyncHistoryExecutorActivate(false)
                    .buildProcessEngine();
        }

        private void initializeFlowableMetadata(JdbcTemplate jdbcTemplate) {
            Map<String, String> properties = Map.ofEntries(
                    Map.entry("common.schema.version", "7.0.0.0"),
                    Map.entry("next.dbid", "1"),
                    Map.entry("identitylink.schema.version", "7.0.0.0"),
                    Map.entry("entitylink.schema.version", "7.0.0.0"),
                    Map.entry("eventsubscription.schema.version", "7.0.0.0"),
                    Map.entry("task.schema.version", "7.0.0.0"),
                    Map.entry("variable.schema.version", "7.0.0.0"),
                    Map.entry("job.schema.version", "7.0.0.0"),
                    Map.entry("batch.schema.version", "7.0.0.0"),
                    Map.entry("schema.version", "7.0.0.0"),
                    Map.entry("schema.history", "create(7.0.0.0)"));
            properties.forEach((name, value) -> jdbcTemplate.update("""
                    insert into ACT_GE_PROPERTY (NAME_, VALUE_, REV_)
                    values (?, ?, 1)
                    on duplicate key update VALUE_ = VALUE_
                    """, name, value));
        }

        @Bean
        RepositoryService repositoryService(ProcessEngine processEngine) {
            return processEngine.getRepositoryService();
        }

        @Bean
        DomainApi domainApi() {
            return new PerformanceDomainApi();
        }
    }

    static final class PerformanceDomainApi implements DomainApi {

        @Override
        public R<PageResult<DomainVO>> page(DomainPageQuery query) {
            return R.ok(PageResult.of(List.of(domain()), 1, 1, 10));
        }

        @Override
        public R<List<DomainVO>> tree(DomainPageQuery query) {
            return R.ok(List.of(domain()));
        }

        @Override
        public R<List<DomainVO>> enabledTree() {
            return R.ok(List.of(domain()));
        }

        @Override
        public R<DomainVO> detail(Long id) {
            return R.ok(domain());
        }

        @Override
        public R<DomainVO> detailByCode(String domainCode) {
            return R.ok(domain());
        }

        @Override
        public R<Long> create(CreateDomainCommand command) {
            return R.ok(1L);
        }

        @Override
        public R<Boolean> update(UpdateDomainCommand command) {
            return R.ok(true);
        }

        @Override
        public R<Boolean> updateStatus(UpdateDomainStatusCommand command) {
            return R.ok(true);
        }

        @Override
        public R<Boolean> delete(Long id) {
            return R.ok(true);
        }

        private static DomainVO domain() {
            DomainVO domain = new DomainVO();
            domain.setId(1L);
            domain.setTenantId("1");
            domain.setDomainCode("PERF");
            domain.setDomainName("性能基准业务域");
            domain.setStatus(1);
            return domain;
        }
    }
}
