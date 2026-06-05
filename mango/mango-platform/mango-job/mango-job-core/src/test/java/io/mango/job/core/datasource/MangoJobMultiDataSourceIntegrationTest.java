package io.mango.job.core.datasource;

import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.infra.persistence.starter.datasource.PersistenceDataSourceContext;
import io.mango.infra.persistence.starter.datasource.PersistenceModuleDataSourceResolver;
import io.mango.job.api.command.SaveMangoJobDefinitionCommand;
import io.mango.job.api.command.TriggerMangoJobCommand;
import io.mango.job.api.command.UpdateMangoJobDefinitionStatusCommand;
import io.mango.job.api.enums.JobDefinitionStatus;
import io.mango.job.api.enums.JobEngineType;
import io.mango.job.api.enums.JobScheduleType;
import io.mango.job.api.enums.JobType;
import io.mango.job.api.handler.MangoJobHandleContext;
import io.mango.job.api.handler.MangoJobHandleResult;
import io.mango.job.api.handler.MangoJobHandler;
import io.mango.job.api.query.MangoJobDefinitionPageQuery;
import io.mango.job.api.query.MangoJobInstancePageQuery;
import io.mango.job.api.query.MangoJobLogPageQuery;
import io.mango.job.api.query.MangoJobWorkerPageQuery;
import io.mango.job.api.vo.MangoJobEngineStatusVO;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobLogIndexEntity;
import io.mango.job.core.entity.MangoJobWorkerSnapshotEntity;
import io.mango.job.core.mapper.MangoJobDefinitionMapper;
import io.mango.job.core.mapper.MangoJobEngineMappingMapper;
import io.mango.job.core.mapper.MangoJobInstanceMapper;
import io.mango.job.core.mapper.MangoJobLogIndexMapper;
import io.mango.job.core.mapper.MangoJobOperationLogMapper;
import io.mango.job.core.mapper.MangoJobWorkerSnapshotMapper;
import io.mango.job.core.service.IMangoJobDefinitionService;
import io.mango.job.core.service.IMangoJobHandlerRegistry;
import io.mango.job.core.service.IMangoJobQueryService;
import io.mango.job.core.service.impl.MangoJobDataSourceRouter;
import io.mango.job.core.service.impl.MangoJobDefinitionService;
import io.mango.job.core.service.impl.MangoJobHandlerRegistry;
import io.mango.job.core.service.impl.MangoJobQueryService;
import io.mango.job.core.service.engine.IMangoJobEngine;
import io.mango.job.core.service.engine.IMangoJobEngineRegistry;
import io.mango.job.core.service.engine.IMangoJobEngineSyncService;
import io.mango.job.core.service.engine.MangoJobEngineRegistry;
import io.mango.job.core.service.engine.MangoJobEngineRequest;
import io.mango.job.core.service.engine.MangoJobEngineResult;
import io.mango.job.core.service.engine.MangoJobEngineSyncService;
import io.mango.job.core.service.engine.MangoJobTriggerRequest;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = MangoJobMultiDataSourceIntegrationTest.TestApplication.class,
        properties = {
                "mango.persistence.datasources.primary.primary=true",
                "mango.persistence.datasources.primary.url=jdbc:h2:mem:mango_job_primary;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "mango.persistence.datasources.primary.driver-class-name=org.h2.Driver",
                "mango.persistence.datasources.primary.username=sa",
                "mango.persistence.datasources.primary.password=",
                "mango.persistence.datasources.job.url=jdbc:h2:mem:mango_job_job;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "mango.persistence.datasources.job.driver-class-name=org.h2.Driver",
                "mango.persistence.datasources.job.username=sa",
                "mango.persistence.datasources.job.password=",
                "mango.persistence.modules.mango-job.datasource=job",
                "mango.persistence.flyway.enabled=true",
                "mango.persistence.mybatis-plus.tenant.enabled=false",
                "mango.persistence.schema-validation.enabled=false"
        }
)
class MangoJobMultiDataSourceIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PersistenceModuleDataSourceResolver resolver;

    @Autowired
    private IMangoJobDefinitionService jobDefinitionService;

    @Autowired
    private IMangoJobQueryService jobQueryService;

    @Autowired
    private MangoJobLogIndexMapper logIndexMapper;

    @Autowired
    private MangoJobWorkerSnapshotMapper workerSnapshotMapper;

    @BeforeEach
    void cleanTables() {
        cleanDatasource("job");
        cleanDatasource("primary");
    }

    @Test
    void flywayAndMybatisPlus_shouldUseConfiguredJobDatasourceForMangoJobModule() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(100L, "tenant-a", "job-test", "test", "user", "tenant", 100L, "internal-admin"));

        assertThat(resolver.resolveDataSource("mango-job")).contains("job");
        assertThat(tableExists("primary", "mango_job_definition")).isFalse();
        assertThat(tableExists("job", "mango_job_definition")).isTrue();

        MangoJobDefinitionEntity entity = new MangoJobDefinitionEntity();
        entity.setId(10001L);
        entity.setTenantId("tenant-a");
        entity.setAppCode("internal-admin");
        entity.setJobCode("sync-user");
        entity.setJobName("Sync User");
        entity.setJobType("BUILTIN");
        entity.setScheduleType("MANUAL");
        entity.setStatus("DRAFT");
        entity.setEngineType("POWERJOB");
        entity.setSyncStatus("PENDING");

        jobDefinitionService.saveDefinition(entity);

        assertThat(rowCount("primary", "mango_job_definition")).isZero();
        assertThat(rowCount("job", "mango_job_definition")).isOne();
        assertThat(jobDefinitionService.findById(10001L).getJobCode()).isEqualTo("sync-user");
    }

    @Test
    void definitionService_shouldCreateUpdateTriggerWithMybatisPlusOnJobDatasource() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-1", "trace-1", "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(200L, "tenant-b", "job-admin", "test", "user", "tenant", 200L, "internal-admin"));

        SaveMangoJobDefinitionCommand command = new SaveMangoJobDefinitionCommand();
        command.setAppCode("internal-admin");
        command.setJobCode("sync-order");
        command.setJobName("Sync Order");
        command.setJobType(JobType.BUILTIN.name());
        command.setScheduleType(JobScheduleType.MANUAL.name());
        command.setHandlerName("syncOrderHandler");
        command.setEngineType(JobEngineType.POWERJOB.name());

        Long id = jobDefinitionService.createDefinition(command);
        assertThat(jobDefinitionService.detailDefinition(id).getStatus()).isEqualTo(JobDefinitionStatus.DRAFT.name());
        assertThat(jobDefinitionService.detailDefinition(id).getSyncStatus()).isEqualTo("SYNCED");
        assertThat(jobDefinitionService.detailDefinition(id).getEngineJobId()).isEqualTo("90001");

        MangoJobDefinitionPageQuery pageQuery = new MangoJobDefinitionPageQuery();
        pageQuery.setKeyword("order");
        assertThat(jobDefinitionService.pageDefinitions(pageQuery).getTotal()).isEqualTo(1);

        UpdateMangoJobDefinitionStatusCommand statusCommand = new UpdateMangoJobDefinitionStatusCommand();
        statusCommand.setId(id);
        statusCommand.setStatus(JobDefinitionStatus.ENABLED.name());
        assertThat(jobDefinitionService.updateDefinitionStatus(statusCommand)).isTrue();

        TriggerMangoJobCommand triggerCommand = new TriggerMangoJobCommand();
        triggerCommand.setJobId(id);
        triggerCommand.setTriggerBatchNo("batch-001");
        Long instanceId = jobDefinitionService.triggerDefinition(triggerCommand);
        assertThat(instanceId).isNotNull();
        assertThat(jobQueryService.pageInstances(new MangoJobInstancePageQuery()).getList().get(0).getEngineInstanceId())
                .isEqualTo("80001");

        MangoJobInstancePageQuery instanceQuery = new MangoJobInstancePageQuery();
        instanceQuery.setJobId(id);
        assertThat(jobQueryService.pageInstances(instanceQuery).getTotal()).isEqualTo(1);

        assertThat(jobQueryService.listHandlers())
                .extracting("handlerName")
                .contains("syncOrderHandler");
        List<MangoJobEngineStatusVO> engineStatus = jobQueryService.listEngineStatus();
        assertThat(engineStatus)
                .filteredOn(item -> JobEngineType.POWERJOB.name().equals(item.getEngineType()))
                .singleElement()
                .extracting(MangoJobEngineStatusVO::getPendingCount)
                .isEqualTo(0L);

        assertThat(rowCount("primary", "mango_job_definition")).isZero();
        assertThat(rowCount("job", "mango_job_definition")).isOne();
        assertThat(rowCount("job", "mango_job_instance")).isOne();
        assertThat(rowCount("job", "mango_job_engine_mapping")).isEqualTo(2);
        assertThat(rowCount("job", "mango_job_operation_log")).isEqualTo(3);
    }

    @Test
    void queryService_shouldReadLogsWorkersAndKeepTenantIsolationOnJobDatasource() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-2", "trace-2", "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(201L, "tenant-b", "job-admin", "test", "user", "tenant", 201L, "internal-admin"));

        Long jobId = jobDefinitionService.createDefinition(definitionCommand("sync-inventory"));
        UpdateMangoJobDefinitionStatusCommand statusCommand = new UpdateMangoJobDefinitionStatusCommand();
        statusCommand.setId(jobId);
        statusCommand.setStatus(JobDefinitionStatus.ENABLED.name());
        jobDefinitionService.updateDefinitionStatus(statusCommand);

        TriggerMangoJobCommand triggerCommand = new TriggerMangoJobCommand();
        triggerCommand.setJobId(jobId);
        Long instanceId = jobDefinitionService.triggerDefinition(triggerCommand);
        insertLogIndex(jobId, instanceId);
        insertWorkerSnapshot();

        assertThat(jobQueryService.pageLogs(new MangoJobLogPageQuery()).getTotal()).isEqualTo(1);
        assertThat(jobQueryService.pageWorkers(new MangoJobWorkerPageQuery()).getTotal()).isEqualTo(1);

        MangoContextHolder.set(MangoContextSnapshot.request("req-3", "trace-3", "tenant-c", "internal-admin", "127.0.0.1")
                .withSecurity(202L, "tenant-c", "job-admin", "test", "user", "tenant", 202L, "internal-admin"));

        assertThat(jobDefinitionService.pageDefinitions(new MangoJobDefinitionPageQuery()).getTotal()).isZero();
        ThrowableAssert.ThrowingCallable detail = () -> jobDefinitionService.detailDefinition(jobId);
        assertThatThrownBy(detail).hasMessageContaining("任务不存在");
        assertThat(jobQueryService.pageInstances(new MangoJobInstancePageQuery()).getTotal()).isZero();
        assertThat(jobQueryService.pageLogs(new MangoJobLogPageQuery()).getTotal()).isZero();
        assertThat(jobQueryService.pageWorkers(new MangoJobWorkerPageQuery()).getTotal()).isZero();
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    private boolean tableExists(String datasource, String tableName) {
        try {
            return query(datasource, () -> jdbcTemplate().queryForObject("""
                    SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.TABLES
                    WHERE TABLE_NAME = ?
                    """, Integer.class, tableName.toLowerCase()) > 0);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private int rowCount(String datasource, String tableName) {
        if (!tableExists(datasource, tableName)) {
            return 0;
        }
        return query(datasource, () -> jdbcTemplate().queryForObject("SELECT COUNT(*) FROM " + tableName,
                Integer.class));
    }

    private void cleanDatasource(String datasource) {
        cleanTable(datasource, "mango_job_operation_log");
        cleanTable(datasource, "mango_job_log_index");
        cleanTable(datasource, "mango_job_instance");
        cleanTable(datasource, "mango_job_engine_mapping");
        cleanTable(datasource, "mango_job_worker_snapshot");
        cleanTable(datasource, "mango_job_alarm_rule");
        cleanTable(datasource, "mango_job_definition");
    }

    private void cleanTable(String datasource, String tableName) {
        if (!tableExists(datasource, tableName)) {
            return;
        }
        query(datasource, () -> {
            jdbcTemplate().execute("DELETE FROM " + tableName);
            return null;
        });
    }

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }

    private <T> T query(String datasource, Supplier<T> supplier) {
        try (PersistenceDataSourceContext.Scope ignored = PersistenceDataSourceContext.use(datasource)) {
            return supplier.get();
        }
    }

    private SaveMangoJobDefinitionCommand definitionCommand(String jobCode) {
        SaveMangoJobDefinitionCommand command = new SaveMangoJobDefinitionCommand();
        command.setAppCode("internal-admin");
        command.setJobCode(jobCode);
        command.setJobName("Sync " + jobCode);
        command.setJobType(JobType.BUILTIN.name());
        command.setScheduleType(JobScheduleType.MANUAL.name());
        command.setHandlerName("syncOrderHandler");
        command.setEngineType(JobEngineType.POWERJOB.name());
        return command;
    }

    private void insertLogIndex(Long jobId, Long instanceId) {
        MangoJobLogIndexEntity entity = new MangoJobLogIndexEntity();
        entity.setTenantId(MangoContextHolder.tenantId());
        entity.setJobId(jobId);
        entity.setInstanceId(instanceId);
        entity.setEngineType(JobEngineType.POWERJOB.name());
        entity.setEngineInstanceId("engine-instance-1");
        entity.setLogLocation("memory://log/1");
        entity.setReadOffset(0L);
        entity.setLastFetchedAt(LocalDateTime.now());
        query("job", () -> logIndexMapper.insert(entity));
    }

    private void insertWorkerSnapshot() {
        MangoJobWorkerSnapshotEntity entity = new MangoJobWorkerSnapshotEntity();
        entity.setTenantId(MangoContextHolder.tenantId());
        entity.setAppCode("internal-admin");
        entity.setWorkerAddress("127.0.0.1:27777");
        entity.setEngineType(JobEngineType.POWERJOB.name());
        entity.setEngineWorkerId("worker-1");
        entity.setLastHeartbeatAt(LocalDateTime.now());
        entity.setStatus("ONLINE");
        query("job", () -> workerSnapshotMapper.insert(entity));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @MapperScan(basePackageClasses = MangoJobDefinitionMapper.class)
    static class TestApplication {

        @Bean
        MangoJobDataSourceRouter mangoJobDataSourceRouter(PersistenceModuleDataSourceResolver resolver) {
            return new MangoJobDataSourceRouter(resolver);
        }

        @Bean
        IMangoJobDefinitionService jobDefinitionService(MangoJobDefinitionMapper mapper,
                                                        MangoJobInstanceMapper instanceMapper,
                                                        MangoJobOperationLogMapper operationLogMapper,
                                                        MangoJobDataSourceRouter dataSourceRouter,
                                                        IMangoJobEngineSyncService engineSyncService) {
            return new MangoJobDefinitionService(mapper, instanceMapper, operationLogMapper, dataSourceRouter,
                    engineSyncService);
        }

        @Bean
        IMangoJobHandlerRegistry jobHandlerRegistry(ObjectProvider<MangoJobHandler> provider) {
            return new MangoJobHandlerRegistry(provider);
        }

        @Bean
        IMangoJobEngineRegistry jobEngineRegistry(ObjectProvider<IMangoJobEngine> provider) {
            return new MangoJobEngineRegistry(provider);
        }

        @Bean
        IMangoJobEngineSyncService jobEngineSyncService(IMangoJobEngineRegistry engineRegistry,
                                                        MangoJobDefinitionMapper definitionMapper,
                                                        MangoJobInstanceMapper instanceMapper,
                                                        MangoJobEngineMappingMapper mappingMapper) {
            return new MangoJobEngineSyncService(engineRegistry, definitionMapper, instanceMapper, mappingMapper);
        }

        @Bean
        IMangoJobQueryService jobQueryService(MangoJobInstanceMapper instanceMapper,
                                              MangoJobLogIndexMapper logIndexMapper,
                                              MangoJobWorkerSnapshotMapper workerSnapshotMapper,
                                              MangoJobDefinitionMapper definitionMapper,
                                              IMangoJobHandlerRegistry handlerRegistry,
                                              MangoJobDataSourceRouter dataSourceRouter) {
            return new MangoJobQueryService(instanceMapper, logIndexMapper, workerSnapshotMapper, definitionMapper,
                    handlerRegistry, dataSourceRouter);
        }

        @Bean
        MangoJobHandler syncOrderHandler() {
            return new MangoJobHandler() {
                @Override
                public String handlerName() {
                    return "syncOrderHandler";
                }

                @Override
                public MangoJobHandleResult handle(MangoJobHandleContext context) {
                    return MangoJobHandleResult.success("ok");
                }
            };
        }

        @Bean
        IMangoJobEngine powerJobTestEngine() {
            return new IMangoJobEngine() {
                @Override
                public String engineType() {
                    return JobEngineType.POWERJOB.name();
                }

                @Override
                public MangoJobEngineResult syncDefinition(MangoJobEngineRequest request) {
                    MangoJobDefinitionEntity definition = request.getDefinition();
                    return MangoJobEngineResult.success("10001", definition.getEngineJobId() == null
                            ? "90001" : definition.getEngineJobId());
                }

                @Override
                public MangoJobEngineResult deleteDefinition(MangoJobEngineRequest request) {
                    return MangoJobEngineResult.success();
                }

                @Override
                public MangoJobEngineResult trigger(MangoJobTriggerRequest request) {
                    return MangoJobEngineResult.triggerSuccess("80001");
                }

                @Override
                public MangoJobEngineResult health() {
                    return MangoJobEngineResult.success();
                }
            };
        }
    }

}
