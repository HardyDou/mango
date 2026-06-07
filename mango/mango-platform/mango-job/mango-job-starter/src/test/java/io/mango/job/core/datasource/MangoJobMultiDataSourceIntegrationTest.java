package io.mango.job.core.datasource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.infra.persistence.starter.datasource.PersistenceDataSourceContext;
import io.mango.infra.persistence.starter.datasource.PersistenceModuleDataSourceResolver;
import io.mango.common.result.R;
import io.mango.job.api.command.CreateMangoJobWorkerCommand;
import io.mango.job.api.command.RegisterMangoJobWorkerCommand;
import io.mango.job.api.command.SaveMangoJobAlarmRuleCommand;
import io.mango.job.api.command.SaveMangoJobDefinitionCommand;
import io.mango.job.api.command.SyncMangoJobInstanceCommand;
import io.mango.job.api.command.TriggerMangoJobCommand;
import io.mango.job.api.command.UpdateMangoJobAlarmRuleStatusCommand;
import io.mango.job.api.command.UpdateMangoJobDefinitionStatusCommand;
import io.mango.job.api.command.UpdateMangoJobWorkerStatusCommand;
import io.mango.job.api.constant.MangoJobNoticeBizTypes;
import io.mango.job.api.enums.JobDefinitionStatus;
import io.mango.job.api.enums.JobEngineType;
import io.mango.job.api.enums.JobScheduleType;
import io.mango.job.api.enums.JobType;
import io.mango.job.api.enums.JobTransportType;
import io.mango.job.api.enums.JobWorkerRegisterSource;
import io.mango.job.api.enums.JobWorkerStatus;
import io.mango.job.api.handler.MangoJobHandleContext;
import io.mango.job.api.handler.MangoJobHandleResult;
import io.mango.job.api.handler.MangoJobHandler;
import io.mango.job.api.query.MangoJobAlarmRulePageQuery;
import io.mango.job.api.query.MangoJobDefinitionPageQuery;
import io.mango.job.api.query.MangoJobInstancePageQuery;
import io.mango.job.api.query.MangoJobLogPageQuery;
import io.mango.job.api.query.MangoJobWorkerPageQuery;
import io.mango.job.api.vo.MangoJobAlarmRuleVO;
import io.mango.job.api.vo.MangoJobEngineStatusVO;
import io.mango.job.api.vo.MangoJobHandlerVO;
import io.mango.job.api.vo.MangoJobInstanceVO;
import io.mango.job.api.vo.MangoJobLogDetailVO;
import io.mango.job.api.vo.MangoJobWorkerExecuteResultVO;
import io.mango.job.core.entity.MangoJobAlarmRuleEntity;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.entity.MangoJobEventEntity;
import io.mango.job.core.entity.MangoJobScheduleCursorEntity;
import io.mango.job.core.entity.MangoJobWorkerCapabilityEntity;
import io.mango.job.core.entity.MangoJobWorkerSnapshotEntity;
import io.mango.job.core.mapper.MangoJobAlarmRuleMapper;
import io.mango.job.core.mapper.MangoJobAttemptMapper;
import io.mango.job.core.mapper.MangoJobDefinitionMapper;
import io.mango.job.core.mapper.MangoJobEngineMappingMapper;
import io.mango.job.core.mapper.MangoJobEventMapper;
import io.mango.job.core.mapper.MangoJobInstanceMapper;
import io.mango.job.core.mapper.MangoJobLogChunkMapper;
import io.mango.job.core.mapper.MangoJobLogIndexMapper;
import io.mango.job.core.mapper.MangoJobOperationLogMapper;
import io.mango.job.core.mapper.MangoJobScheduleCursorMapper;
import io.mango.job.core.mapper.MangoJobWorkerCapabilityMapper;
import io.mango.job.core.mapper.MangoJobWorkerSnapshotMapper;
import io.mango.job.core.service.IMangoJobAlarmRuleService;
import io.mango.job.core.service.IMangoJobDefinitionService;
import io.mango.job.core.service.IMangoJobQueryService;
import io.mango.job.core.service.IMangoJobWorkerRegistryService;
import io.mango.job.core.service.nativeengine.IMangoNativeJobRuntime;
import io.mango.job.support.nativeengine.IMangoJobWorkerTransport;
import io.mango.job.support.nativeengine.MangoJobWorkerDispatchRequest;
import io.mango.job.support.nativeengine.MangoNativeJobProperties;
import io.mango.job.starter.MangoEmbeddedWorkerRegistrar;
import io.mango.notice.api.NoticeApi;
import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.vo.NoticeSendResultVO;
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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.IntStream;

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
                "mango.persistence.schema-validation.enabled=false",
                "mango.job.native.app-code=internal-admin",
                "spring.autoconfigure.exclude=io.mango.job.starter.remote.JobRemoteAutoConfiguration"
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
    private IMangoJobAlarmRuleService alarmRuleService;

    @Autowired
    private IMangoJobQueryService jobQueryService;

    @Autowired
    private IMangoNativeJobRuntime nativeJobRuntime;

    @Autowired
    private MangoJobLogIndexMapper logIndexMapper;

    @Autowired
    private MangoJobLogChunkMapper logChunkMapper;

    @Autowired
    private MangoJobWorkerSnapshotMapper workerSnapshotMapper;

    @Autowired
    private MangoJobScheduleCursorMapper scheduleCursorMapper;

    @Autowired
    private MangoJobWorkerCapabilityMapper workerCapabilityMapper;

    @Autowired
    private MangoJobEventMapper eventMapper;

    @Autowired
    private MangoJobAlarmRuleMapper alarmRuleMapper;

    @Autowired
    private IMangoJobWorkerRegistryService workerRegistryService;

    @Autowired
    private MangoNativeJobProperties nativeProperties;

    @Autowired
    private MangoEmbeddedWorkerRegistrar embeddedWorkerRegistrar;

    @BeforeEach
    void cleanTables() {
        nativeProperties.setEmbeddedWorkerEnabled(true);
        nativeProperties.setTransport(JobTransportType.IN_MEMORY);
        nativeProperties.setSchedulerTenantId("1");
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
        entity.setOwnerService("internal-admin");
        entity.setWorkerGroup("internal-admin");
        entity.setJobCode("sync-user");
        entity.setJobName("Sync User");
        entity.setJobType("BUILTIN");
        entity.setScheduleType("MANUAL");
        entity.setStatus("DRAFT");
        entity.setEngineType(JobEngineType.MANGO_NATIVE.name());
        entity.setSyncStatus("PENDING");

        jobDefinitionService.saveDefinition(entity);

        assertThat(rowCount("primary", "mango_job_definition")).isZero();
        assertThat(rowCount("job", "mango_job_definition")).isOne();
        assertThat(jobDefinitionService.findById(10001L).getJobCode()).isEqualTo("sync-user");
    }

    @Test
    void flywayAndMybatisPlus_shouldCreateNativeEngineTablesAndIndexesOnJobDatasource() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(101L, "tenant-a", "job-test", "test", "user", "tenant", 101L, "internal-admin"));

        assertThat(tableExists("primary", "mango_job_schedule_cursor")).isFalse();
        assertThat(tableExists("job", "mango_job_schedule_cursor")).isTrue();
        assertThat(tableExists("job", "mango_job_attempt")).isTrue();
        assertThat(tableExists("job", "mango_job_worker_capability")).isTrue();
        assertThat(tableExists("job", "mango_job_log_chunk")).isTrue();
        assertThat(tableExists("job", "mango_job_event")).isTrue();
        assertThat(columnExists("job", "mango_job_definition", "owner_service")).isTrue();
        assertThat(columnExists("job", "mango_job_worker_snapshot", "service_code")).isTrue();
        assertThat(columnExists("job", "mango_job_worker_capability", "job_code")).isTrue();
        assertThat(constraintOrIndexExists("job", "mango_job_schedule_cursor", "uk_schedule_cursor_job")).isTrue();
        assertThat(constraintOrIndexExists("job", "mango_job_instance", "uk_instance_idempotency")).isTrue();
        assertThat(constraintOrIndexExists("job", "mango_job_log_chunk", "uk_log_chunk_sequence")).isTrue();
        assertThat(constraintOrIndexExists("job", "mango_job_worker_snapshot", "uk_worker_owner_engine_address")).isTrue();
        assertThat(constraintOrIndexExists("job", "mango_job_worker_capability", "uk_worker_owner_handler")).isTrue();

        MangoJobEventEntity event = new MangoJobEventEntity();
        event.setTenantId("tenant-a");
        event.setJobId(10002L);
        event.setEventType("INSTANCE_CREATED");
        event.setEventTime(LocalDateTime.now());
        event.setTraceId("trace-migration");
        event.setPayload("{\"source\":\"migration-test\"}");

        query("job", () -> eventMapper.insert(event));

        assertThat(rowCount("primary", "mango_job_event")).isZero();
        assertThat(query("job", () -> eventMapper.selectById(event.getId())))
                .isNotNull()
                .satisfies(saved -> {
                    assertThat(saved.getTenantId()).isEqualTo("tenant-a");
                    assertThat(saved.getEventType()).isEqualTo("INSTANCE_CREATED");
                    assertThat(saved.getPayload()).contains("migration-test");
                });
    }

    @Test
    void definitionService_shouldCreateUpdateTriggerWithMybatisPlusOnJobDatasource() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-1", "trace-1", "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(200L, "tenant-b", "job-admin", "test", "user", "tenant", 200L, "internal-admin"));

        SaveMangoJobDefinitionCommand command = new SaveMangoJobDefinitionCommand();
        command.setAppCode("internal-admin");
        command.setOwnerService("internal-admin");
        command.setWorkerGroup("internal-admin");
        command.setJobCode("sync-order");
        command.setJobName("Sync Order");
        command.setJobType(JobType.BUILTIN.name());
        command.setScheduleType(JobScheduleType.MANUAL.name());
        command.setHandlerName("syncOrderHandler");
        command.setEngineType(JobEngineType.MANGO_NATIVE.name());

        Long id = jobDefinitionService.createDefinition(command);
        assertThat(jobDefinitionService.detailDefinition(id).getStatus()).isEqualTo(JobDefinitionStatus.DRAFT.name());
        assertThat(jobDefinitionService.detailDefinition(id).getOwnerService()).isEqualTo("internal-admin");
        assertThat(jobDefinitionService.detailDefinition(id).getWorkerGroup()).isEqualTo("internal-admin");
        assertThat(jobDefinitionService.detailDefinition(id).getSyncStatus()).isEqualTo("SYNCED");
        assertThat(jobDefinitionService.detailDefinition(id).getEngineJobId()).isEqualTo(String.valueOf(id));

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
                .isEqualTo(String.valueOf(instanceId));
        assertThat(jobQueryService.pageInstances(new MangoJobInstancePageQuery()).getList().get(0).getJobCode())
                .isEqualTo("sync-order");
        assertThat(jobQueryService.pageInstances(new MangoJobInstancePageQuery()).getList().get(0).getJobName())
                .isEqualTo("Sync Order");

        MangoJobInstancePageQuery instanceQuery = new MangoJobInstancePageQuery();
        instanceQuery.setJobId(id);
        assertThat(jobQueryService.pageInstances(instanceQuery).getTotal()).isEqualTo(1);

        assertThat(jobQueryService.listHandlers())
                .extracting("handlerName")
                .contains("syncOrderHandler");
        List<MangoJobEngineStatusVO> engineStatus = jobQueryService.listEngineStatus();
        assertThat(engineStatus)
                .filteredOn(item -> JobEngineType.MANGO_NATIVE.name().equals(item.getEngineType()))
                .singleElement()
                .extracting(MangoJobEngineStatusVO::getPendingCount)
                .isEqualTo(0L);

        assertThat(rowCount("primary", "mango_job_definition")).isZero();
        assertThat(rowCount("job", "mango_job_definition")).isOne();
        assertThat(rowCount("job", "mango_job_instance")).isOne();
        assertThat(rowCount("job", "mango_job_engine_mapping")).isEqualTo(2);
        assertThat(rowCount("job", "mango_job_log_index")).isOne();
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

        assertThat(jobQueryService.pageLogs(new MangoJobLogPageQuery()).getTotal()).isEqualTo(1);
        assertThat(jobQueryService.pageWorkers(new MangoJobWorkerPageQuery()).getList())
                .singleElement()
                .extracting("workerAddress")
                .asString()
                .startsWith("in-memory://");

        MangoContextHolder.set(MangoContextSnapshot.request("req-3", "trace-3", "tenant-c", "internal-admin", "127.0.0.1")
                .withSecurity(202L, "tenant-c", "job-admin", "test", "user", "tenant", 202L, "internal-admin"));

        assertThat(jobDefinitionService.pageDefinitions(new MangoJobDefinitionPageQuery()).getTotal()).isZero();
        ThrowableAssert.ThrowingCallable detail = () -> jobDefinitionService.detailDefinition(jobId);
        assertThatThrownBy(detail).hasMessageContaining("任务不存在");
        assertThat(jobQueryService.pageInstances(new MangoJobInstancePageQuery()).getTotal()).isZero();
        assertThat(jobQueryService.pageLogs(new MangoJobLogPageQuery()).getTotal()).isZero();
        assertThat(jobQueryService.pageWorkers(new MangoJobWorkerPageQuery()).getTotal()).isZero();
    }

    @Test
    void queryService_shouldNotImportScheduledInstancesFromExternalEngineForNativeDefinitions() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-6", "trace-6", "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(205L, "tenant-b", "job-admin", "test", "user", "tenant", 205L, "internal-admin"));

        SaveMangoJobDefinitionCommand command = definitionCommand("sync-scheduled");
        command.setScheduleType(JobScheduleType.CRON.name());
        command.setScheduleExpression("0 */1 * * * ?");
        Long jobId = jobDefinitionService.createDefinition(command);
        UpdateMangoJobDefinitionStatusCommand statusCommand = new UpdateMangoJobDefinitionStatusCommand();
        statusCommand.setId(jobId);
        statusCommand.setStatus(JobDefinitionStatus.ENABLED.name());
        jobDefinitionService.updateDefinitionStatus(statusCommand);

        MangoJobInstancePageQuery query = new MangoJobInstancePageQuery();
        query.setJobId(jobId);
        assertThat(jobQueryService.pageInstances(query).getList()).isEmpty();
        assertThat(jobQueryService.pageLogs(new MangoJobLogPageQuery()).getTotal()).isZero();
        assertThat(rowCount("job", "mango_job_instance")).isZero();
        assertThat(rowCount("job", "mango_job_log_index")).isZero();
        assertThat(rowCount("job", "mango_job_engine_mapping")).isOne();
    }

    @Test
    void embeddedWorkerRegistrar_shouldAutoRegisterLocalHandlersWithoutDispatch() {
        nativeProperties.setSchedulerTenantId("tenant-b");
        MangoContextHolder.set(MangoContextSnapshot.request("req-worker-auto", "trace-worker-auto",
                        "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(207L, "tenant-b", "job-admin", "test", "user", "tenant", 207L, "internal-admin"));

        embeddedWorkerRegistrar.registerOnReady();

        assertThat(rowCount("job", "mango_job_instance")).isZero();
        assertThat(jobQueryService.pageWorkers(new MangoJobWorkerPageQuery()).getList())
                .singleElement()
                .satisfies(worker -> {
                    assertThat(worker.getWorkerAddress()).startsWith("in-memory://");
                    assertThat(worker.getRuntimeAddress()).startsWith("in-memory://");
                    assertThat(worker.getTransportType()).isEqualTo(JobTransportType.IN_MEMORY.name());
                    assertThat(worker.getRegisterSource()).isEqualTo(JobWorkerRegisterSource.EMBEDDED_AUTO.name());
                    assertThat(worker.getServiceCode()).isEqualTo("internal-admin");
                    assertThat(worker.getWorkerGroup()).isEqualTo("internal-admin");
                    assertThat(worker.getStatus()).isEqualTo(JobWorkerStatus.ONLINE.name());
                });
        assertThat(query("job", () -> workerCapabilityMapper.selectList(
                new LambdaQueryWrapper<MangoJobWorkerCapabilityEntity>()
                        .eq(MangoJobWorkerCapabilityEntity::getTenantId, "tenant-b")
                        .eq(MangoJobWorkerCapabilityEntity::getHandlerName, "syncOrderHandler")
                        .eq(MangoJobWorkerCapabilityEntity::getServiceCode, "internal-admin")
                        .eq(MangoJobWorkerCapabilityEntity::getWorkerGroup, "internal-admin")
                        .eq(MangoJobWorkerCapabilityEntity::getEnabled, 1))))
                .hasSize(1);
    }

    @Test
    void nativeRuntime_shouldExecuteInMemoryWorkerAndPersistLogsOnJobDatasource() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-native", "trace-native", "tenant-b",
                        "internal-admin", "127.0.0.1")
                .withSecurity(208L, "tenant-b", "job-admin", "test", "user", "tenant", 208L, "internal-admin"));

        SaveMangoJobDefinitionCommand command = definitionCommand("native-probe");
        command.setEngineType(JobEngineType.MANGO_NATIVE.name());
        command.setParamValue("{\"scene\":\"native-manual\"}");
        Long jobId = jobDefinitionService.createDefinition(command);
        UpdateMangoJobDefinitionStatusCommand statusCommand = new UpdateMangoJobDefinitionStatusCommand();
        statusCommand.setId(jobId);
        statusCommand.setStatus(JobDefinitionStatus.ENABLED.name());
        jobDefinitionService.updateDefinitionStatus(statusCommand);

        TriggerMangoJobCommand triggerCommand = new TriggerMangoJobCommand();
        triggerCommand.setJobId(jobId);
        triggerCommand.setTriggerBatchNo("native-batch-001");
        Long instanceId = jobDefinitionService.triggerDefinition(triggerCommand);

        assertThat(jobQueryService.pageInstances(new MangoJobInstancePageQuery()).getList())
                .singleElement()
                .satisfies(instance -> {
                    assertThat(instance.getJobCode()).isEqualTo("native-probe");
                    assertThat(instance.getJobName()).isEqualTo("Sync native-probe");
                    assertThat(instance.getStatus()).isEqualTo("SUCCESS");
                    assertThat(instance.getEngineType()).isEqualTo(JobEngineType.MANGO_NATIVE.name());
                    assertThat(instance.getAttemptCount()).isEqualTo(1);
                    assertThat(instance.getResultSummary()).isEqualTo("ok");
                });
        MangoJobLogDetailVO detail = jobQueryService.detailInstanceLog(instanceId);
        assertThat(detail.getLogSource()).isEqualTo(JobEngineType.MANGO_NATIVE.name());
        assertThat(detail.getContent()).contains("handlerResult=ok");
        assertThat(detail.getContent()).contains("syncOrderHandler System.out");
        assertThat(detail.getContent()).contains("syncOrderHandler logger");
        assertThat(detail.getContent()).contains("Job attempt leased by in-memory://");
        assertThat(query("job", () -> logChunkMapper.selectList(
                new LambdaQueryWrapper<>()).stream()
                .filter(log -> "System.out".equals(log.getLoggerName()))
                .filter(log -> log.getContent().contains("syncOrderHandler logger"))
                .count())).isZero();
        assertThat(query("job", () -> logChunkMapper.selectList(
                new LambdaQueryWrapper<>()).stream()
                .filter(log -> !"System.out".equals(log.getLoggerName()))
                .filter(log -> log.getContent().contains("syncOrderHandler logger"))
                .count())).isOne();
        assertThat(rowCount("job", "mango_job_attempt")).isOne();
        assertThat(rowCount("job", "mango_job_log_chunk")).isGreaterThanOrEqualTo(2);
        assertThat(jobQueryService.pageWorkers(new MangoJobWorkerPageQuery()).getList())
                .singleElement()
                .satisfies(worker -> {
                    assertThat(worker.getEngineType()).isEqualTo(JobEngineType.MANGO_NATIVE.name());
                    assertThat(worker.getWorkerAddress()).startsWith("in-memory://");
                    assertThat(worker.getServiceCode()).isEqualTo("internal-admin");
                    assertThat(worker.getWorkerGroup()).isEqualTo("internal-admin");
                    assertThat(worker.getTransportType()).isEqualTo(JobTransportType.IN_MEMORY.name());
                    assertThat(worker.getRegisterSource()).isEqualTo(JobWorkerRegisterSource.EMBEDDED_AUTO.name());
                    assertThat(worker.getStatus()).isEqualTo("ONLINE");
                });
    }

    @Test
    void nativeRuntime_shouldCreateScheduledInstanceWhenCursorDue() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-native-schedule", "trace-native-schedule",
                        "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(209L, "tenant-b", "job-admin", "test", "user", "tenant", 209L, "internal-admin"));

        SaveMangoJobDefinitionCommand command = definitionCommand("native-every-minute");
        command.setEngineType(JobEngineType.MANGO_NATIVE.name());
        command.setScheduleType(JobScheduleType.FIXED_RATE.name());
        command.setScheduleExpression("60000");
        command.setParamValue("{\"scene\":\"every-minute\"}");
        Long jobId = jobDefinitionService.createDefinition(command);
        UpdateMangoJobDefinitionStatusCommand statusCommand = new UpdateMangoJobDefinitionStatusCommand();
        statusCommand.setId(jobId);
        statusCommand.setStatus(JobDefinitionStatus.ENABLED.name());
        jobDefinitionService.updateDefinitionStatus(statusCommand);
        query("job", () -> {
            MangoJobScheduleCursorEntity cursor = scheduleCursorMapper.selectOne(
                    new LambdaQueryWrapper<MangoJobScheduleCursorEntity>()
                            .eq(MangoJobScheduleCursorEntity::getJobId, jobId)
                            .last("limit 1"));
            cursor.setNextFireTime(LocalDateTime.now().minusSeconds(1));
            scheduleCursorMapper.updateById(cursor);
            return null;
        });

        nativeJobRuntime.tick();

        MangoJobInstancePageQuery query = new MangoJobInstancePageQuery();
        query.setJobId(jobId);
        assertThat(jobQueryService.pageInstances(query).getList())
                .singleElement()
                .satisfies(instance -> {
                    assertThat(instance.getJobCode()).isEqualTo("native-every-minute");
                    assertThat(instance.getTriggerType()).isEqualTo("SCHEDULED");
                    assertThat(instance.getStatus()).isEqualTo("SUCCESS");
                    assertThat(instance.getScheduledFireTime()).isNotNull();
                    assertThat(instance.getActualFireTime()).isNotNull();
                });
        assertThat(rowCount("job", "mango_job_schedule_cursor")).isOne();
        assertThat(rowCount("job", "mango_job_attempt")).isOne();
        assertThat(rowCount("job", "mango_job_log_chunk")).isGreaterThanOrEqualTo(2);
    }

    @Test
    void nativeRuntime_shouldContinueScheduleCursorAfterJobCenterRestartWithoutDuplicatingCompletedWindow() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-native-restart", "trace-native-restart",
                        "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(214L, "tenant-b", "job-admin", "test", "user", "tenant", 214L, "internal-admin"));

        SaveMangoJobDefinitionCommand command = definitionCommand("native-restart-recovery");
        command.setEngineType(JobEngineType.MANGO_NATIVE.name());
        command.setScheduleType(JobScheduleType.FIXED_RATE.name());
        command.setScheduleExpression("60000");
        command.setParamValue("{\"scene\":\"restart-recovery\"}");
        Long jobId = jobDefinitionService.createDefinition(command);
        UpdateMangoJobDefinitionStatusCommand statusCommand = new UpdateMangoJobDefinitionStatusCommand();
        statusCommand.setId(jobId);
        statusCommand.setStatus(JobDefinitionStatus.ENABLED.name());
        jobDefinitionService.updateDefinitionStatus(statusCommand);
        LocalDateTime firstFireTime = LocalDateTime.now().minusMinutes(2).withNano(0);
        query("job", () -> {
            MangoJobScheduleCursorEntity cursor = scheduleCursorMapper.selectOne(
                    new LambdaQueryWrapper<MangoJobScheduleCursorEntity>()
                            .eq(MangoJobScheduleCursorEntity::getJobId, jobId)
                            .last("limit 1"));
            cursor.setLastFireTime(null);
            cursor.setNextFireTime(firstFireTime);
            cursor.setLockOwner(null);
            cursor.setLockUntil(null);
            scheduleCursorMapper.updateById(cursor);
            return null;
        });

        nativeJobRuntime.tick();

        MangoJobScheduleCursorEntity cursorAfterFirstTick = query("job", () -> scheduleCursorMapper.selectOne(
                new LambdaQueryWrapper<MangoJobScheduleCursorEntity>()
                        .eq(MangoJobScheduleCursorEntity::getJobId, jobId)
                        .last("limit 1")));
        assertThat(cursorAfterFirstTick.getLastFireTime()).isEqualTo(firstFireTime);
        assertThat(cursorAfterFirstTick.getNextFireTime()).isAfter(cursorAfterFirstTick.getLastFireTime());
        MangoJobInstancePageQuery instanceQuery = new MangoJobInstancePageQuery();
        instanceQuery.setJobId(jobId);
        assertThat(jobQueryService.pageInstances(instanceQuery).getList())
                .singleElement()
                .satisfies(instance -> {
                    assertThat(instance.getTriggerType()).isEqualTo("SCHEDULED");
                    assertThat(instance.getScheduledFireTime()).isEqualTo(firstFireTime);
                    assertThat(instance.getStatus()).isEqualTo("SUCCESS");
                });

        nativeJobRuntime.tick();

        List<MangoJobInstanceVO> instances = jobQueryService.pageInstances(instanceQuery).getList();
        assertThat(instances).hasSize(2);
        assertThat(instances.stream().map(MangoJobInstanceVO::getScheduledFireTime).toList())
                .contains(firstFireTime, cursorAfterFirstTick.getNextFireTime())
                .doesNotHaveDuplicates();
        MangoJobScheduleCursorEntity cursorAfterRestartTick = query("job", () -> scheduleCursorMapper.selectOne(
                new LambdaQueryWrapper<MangoJobScheduleCursorEntity>()
                        .eq(MangoJobScheduleCursorEntity::getJobId, jobId)
                        .last("limit 1")));
        assertThat(cursorAfterRestartTick.getLastFireTime()).isEqualTo(cursorAfterFirstTick.getNextFireTime());
        assertThat(rowCount("job", "mango_job_attempt")).isEqualTo(2);
    }

    @Test
    void nativeRuntime_shouldKeepEveryMinuteCronStableAcrossContinuousWindows() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-native-cron-stability", "trace-native-cron-stability",
                        "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(215L, "tenant-b", "job-admin", "test", "user", "tenant", 215L, "internal-admin"));

        SaveMangoJobDefinitionCommand command = definitionCommand("native-cron-stability");
        command.setEngineType(JobEngineType.MANGO_NATIVE.name());
        command.setScheduleType(JobScheduleType.CRON.name());
        command.setScheduleExpression("0 */1 * * * ?");
        command.setParamValue("{\"scene\":\"cron-stability\"}");
        Long jobId = jobDefinitionService.createDefinition(command);
        UpdateMangoJobDefinitionStatusCommand statusCommand = new UpdateMangoJobDefinitionStatusCommand();
        statusCommand.setId(jobId);
        statusCommand.setStatus(JobDefinitionStatus.ENABLED.name());
        jobDefinitionService.updateDefinitionStatus(statusCommand);
        LocalDateTime firstFireTime = LocalDateTime.now().minusMinutes(5).withSecond(0).withNano(0);
        List<LocalDateTime> expectedFireTimes = List.of(
                firstFireTime,
                firstFireTime.plusMinutes(1),
                firstFireTime.plusMinutes(2),
                firstFireTime.plusMinutes(3),
                firstFireTime.plusMinutes(4));
        query("job", () -> {
            MangoJobScheduleCursorEntity cursor = scheduleCursorMapper.selectOne(
                    new LambdaQueryWrapper<MangoJobScheduleCursorEntity>()
                            .eq(MangoJobScheduleCursorEntity::getJobId, jobId)
                            .last("limit 1"));
            cursor.setLastFireTime(null);
            cursor.setNextFireTime(firstFireTime);
            cursor.setLockOwner(null);
            cursor.setLockUntil(null);
            scheduleCursorMapper.updateById(cursor);
            return null;
        });

        for (int i = 0; i < expectedFireTimes.size(); i++) {
            nativeJobRuntime.tick();
        }

        MangoJobInstancePageQuery instanceQuery = new MangoJobInstancePageQuery();
        instanceQuery.setJobId(jobId);
        List<MangoJobInstanceVO> instances = jobQueryService.pageInstances(instanceQuery).getList();
        assertThat(instances).hasSize(expectedFireTimes.size())
                .allSatisfy(instance -> {
                    assertThat(instance.getTriggerType()).isEqualTo("SCHEDULED");
                    assertThat(instance.getStatus()).isEqualTo("SUCCESS");
                    assertThat(instance.getJobCode()).isEqualTo("native-cron-stability");
                    assertThat(instance.getWorkerAddress()).startsWith("in-memory://");
                });
        assertThat(instances.stream().map(MangoJobInstanceVO::getScheduledFireTime).sorted().toList())
                .containsExactlyElementsOf(expectedFireTimes);
        assertThat(instances.stream().map(MangoJobInstanceVO::getTriggerBatchNo).toList())
                .doesNotHaveDuplicates();
        MangoJobScheduleCursorEntity cursorAfterTicks = query("job", () -> scheduleCursorMapper.selectOne(
                new LambdaQueryWrapper<MangoJobScheduleCursorEntity>()
                        .eq(MangoJobScheduleCursorEntity::getJobId, jobId)
                        .last("limit 1")));
        assertThat(cursorAfterTicks.getLastFireTime()).isEqualTo(expectedFireTimes.get(expectedFireTimes.size() - 1));
        assertThat(cursorAfterTicks.getNextFireTime()).isEqualTo(firstFireTime.plusMinutes(5));
        assertThat(rowCount("job", "mango_job_attempt")).isEqualTo(expectedFireTimes.size());
        assertThat(rowCount("job", "mango_job_log_index")).isEqualTo(expectedFireTimes.size());
        assertThat(rowCount("job", "mango_job_log_chunk")).isGreaterThanOrEqualTo(expectedFireTimes.size() * 2);
    }

    @Test
    void nativeRuntime_shouldCreateOnlyOneScheduledInstanceWhenTwoJobCentersTickSameCursor() throws Exception {
        MangoContextSnapshot snapshot = MangoContextSnapshot.request("req-native-race", "trace-native-race",
                        "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(211L, "tenant-b", "job-admin", "test", "user", "tenant", 211L, "internal-admin");
        MangoContextHolder.set(snapshot);

        SaveMangoJobDefinitionCommand command = definitionCommand("native-race-every-minute");
        command.setEngineType(JobEngineType.MANGO_NATIVE.name());
        command.setScheduleType(JobScheduleType.FIXED_RATE.name());
        command.setScheduleExpression("60000");
        command.setParamValue("{\"scene\":\"race\"}");
        Long jobId = jobDefinitionService.createDefinition(command);
        UpdateMangoJobDefinitionStatusCommand statusCommand = new UpdateMangoJobDefinitionStatusCommand();
        statusCommand.setId(jobId);
        statusCommand.setStatus(JobDefinitionStatus.ENABLED.name());
        jobDefinitionService.updateDefinitionStatus(statusCommand);
        query("job", () -> {
            MangoJobScheduleCursorEntity cursor = scheduleCursorMapper.selectOne(
                    new LambdaQueryWrapper<MangoJobScheduleCursorEntity>()
                            .eq(MangoJobScheduleCursorEntity::getJobId, jobId)
                            .last("limit 1"));
            cursor.setNextFireTime(LocalDateTime.now().minusSeconds(1));
            scheduleCursorMapper.updateById(cursor);
            return null;
        });

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> runTickWithContext(snapshot, ready, start));
            Future<?> second = executor.submit(() -> runTickWithContext(snapshot, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        MangoJobInstancePageQuery query = new MangoJobInstancePageQuery();
        query.setJobId(jobId);
        assertThat(jobQueryService.pageInstances(query).getList())
                .singleElement()
                .satisfies(instance -> {
                    assertThat(instance.getJobCode()).isEqualTo("native-race-every-minute");
                    assertThat(instance.getTriggerType()).isEqualTo("SCHEDULED");
                    assertThat(instance.getStatus()).isEqualTo("SUCCESS");
                    assertThat(instance.getTriggerBatchNo()).startsWith("schedule-" + jobId);
                });
        assertThat(rowCount("job", "mango_job_attempt")).isOne();
    }

    @Test
    void embeddedWorkers_shouldRegisterMultipleInMemoryInstancesAndDispatchOnlyByCapability() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-embedded-multi", "trace-embedded-multi",
                        "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(225L, "tenant-b", "job-admin", "test", "user", "tenant", 225L, "internal-admin"));

        nativeProperties.setSchedulerTenantId("tenant-b");
        embeddedWorkerRegistrar.registerOnReady();
        registerEmbeddedWorker("in-memory://test-node/embedded-second-instance",
                "embedded-second-instance", "syncOrderHandler", "internal-admin", "internal-admin");
        registerEmbeddedWorker("in-memory://test-node/embedded-service-b-instance",
                "embedded-service-b-instance", "syncOrderHandler", "service-b", "service-b");

        List<MangoJobWorkerSnapshotEntity> embeddedWorkers = query("job", () -> workerSnapshotMapper.selectList(
                new LambdaQueryWrapper<MangoJobWorkerSnapshotEntity>()
                        .eq(MangoJobWorkerSnapshotEntity::getTenantId, "tenant-b")
                        .eq(MangoJobWorkerSnapshotEntity::getTransportType, JobTransportType.IN_MEMORY.name())
                        .eq(MangoJobWorkerSnapshotEntity::getRegisterSource,
                                JobWorkerRegisterSource.EMBEDDED_AUTO.name())
                        .orderByAsc(MangoJobWorkerSnapshotEntity::getServiceCode)
                        .orderByAsc(MangoJobWorkerSnapshotEntity::getWorkerAddress)));
        assertThat(embeddedWorkers)
                .hasSize(3)
                .allSatisfy(worker -> {
                    assertThat(worker.getStatus()).isEqualTo(JobWorkerStatus.ONLINE.name());
                    assertThat(worker.getWorkerAddress()).startsWith("in-memory://");
                });
        assertThat(embeddedWorkers)
                .filteredOn(worker -> "internal-admin".equals(worker.getServiceCode()))
                .hasSize(2);
        assertThat(embeddedWorkers)
                .filteredOn(worker -> "service-b".equals(worker.getServiceCode()))
                .singleElement()
                .satisfies(worker -> assertThat(worker.getWorkerGroup()).isEqualTo("service-b"));

        SaveMangoJobDefinitionCommand command = definitionCommand("embedded-multi-owner-job");
        command.setParamValue("{\"scene\":\"embedded-multi\"}");
        Long jobId = jobDefinitionService.createDefinition(command);
        UpdateMangoJobDefinitionStatusCommand statusCommand = new UpdateMangoJobDefinitionStatusCommand();
        statusCommand.setId(jobId);
        statusCommand.setStatus(JobDefinitionStatus.ENABLED.name());
        jobDefinitionService.updateDefinitionStatus(statusCommand);

        TriggerMangoJobCommand triggerCommand = new TriggerMangoJobCommand();
        triggerCommand.setJobId(jobId);
        triggerCommand.setTriggerBatchNo("embedded-multi-owner-batch");
        Long instanceId = jobDefinitionService.triggerDefinition(triggerCommand);

        assertThat(jobQueryService.pageInstances(new MangoJobInstancePageQuery()).getList())
                .singleElement()
                .satisfies(instance -> {
                    assertThat(instance.getId()).isEqualTo(instanceId);
                    assertThat(instance.getStatus()).isEqualTo("SUCCESS");
                    assertThat(instance.getWorkerAddress()).startsWith("in-memory://");
                });
        Long workerId = query("job", () -> jdbcTemplate().queryForObject(
                "SELECT worker_id FROM mango_job_attempt WHERE instance_id = ?",
                Long.class, instanceId));
        assertThat(query("job", () -> workerSnapshotMapper.selectById(workerId)))
                .satisfies(worker -> {
                    assertThat(worker.getServiceCode()).isEqualTo("internal-admin");
                    assertThat(worker.getWorkerGroup()).isEqualTo("internal-admin");
                });
    }

    @Test
    void nativeRuntime_shouldDispatchToHttpInternalWorkerWhenEmbeddedWorkerDisabled() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-native-http", "trace-native-http",
                        "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(210L, "tenant-b", "job-admin", "test", "user", "tenant", 210L, "internal-admin"));

        SaveMangoJobDefinitionCommand command = definitionCommand("native-http-worker");
        command.setEngineType(JobEngineType.MANGO_NATIVE.name());
        command.setParamValue("{\"scene\":\"http-internal\"}");
        httpInternalTransport.lastWorkerAddress = null;
        httpInternalTransport.lastParameter = null;
        nativeProperties.setEmbeddedWorkerEnabled(false);
        try {
            Long jobId = jobDefinitionService.createDefinition(command);
            registerNativeWorker("http://worker-a:8080", "syncOrderHandler");
            UpdateMangoJobDefinitionStatusCommand statusCommand = new UpdateMangoJobDefinitionStatusCommand();
            statusCommand.setId(jobId);
            statusCommand.setStatus(JobDefinitionStatus.ENABLED.name());
            jobDefinitionService.updateDefinitionStatus(statusCommand);

            TriggerMangoJobCommand triggerCommand = new TriggerMangoJobCommand();
            triggerCommand.setJobId(jobId);
            triggerCommand.setTriggerBatchNo("native-http-batch-001");
            Long instanceId = jobDefinitionService.triggerDefinition(triggerCommand);

            assertThat(httpInternalTransport.lastWorkerAddress).isEqualTo("http://worker-a:8080");
            assertThat(httpInternalTransport.lastParameter).contains("http-internal");
            assertThat(jobQueryService.pageInstances(new MangoJobInstancePageQuery()).getList())
                    .singleElement()
                    .satisfies(instance -> {
                        assertThat(instance.getId()).isEqualTo(instanceId);
                        assertThat(instance.getStatus()).isEqualTo("SUCCESS");
                        assertThat(instance.getWorkerAddress()).isEqualTo("http://worker-a:8080");
                        assertThat(instance.getResultSummary()).isEqualTo("remote-ok");
                    });
            MangoJobLogDetailVO detail = jobQueryService.detailInstanceLog(instanceId);
            assertThat(detail.getContent()).contains("remote worker stdout");
            assertThat(detail.getContent()).contains("handlerResult=remote-ok");
        } finally {
            nativeProperties.setEmbeddedWorkerEnabled(true);
        }
    }

    @Test
    void workerRegistry_shouldKeepRegistrationIdempotentWhenWorkerHeartbeatRepeats() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-worker-register", "trace-worker-register",
                        "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(213L, "tenant-b", "job-admin", "test", "user", "tenant", 213L, "internal-admin"));

        registerNativeWorker("http://worker-idempotent:8080", "syncOrderHandler");
        registerNativeWorker("http://worker-idempotent:8080", "otherHandler");

        assertThat(query("job", () -> workerSnapshotMapper.selectList(
                new LambdaQueryWrapper<MangoJobWorkerSnapshotEntity>()
                        .eq(MangoJobWorkerSnapshotEntity::getTenantId, "tenant-b")
                        .eq(MangoJobWorkerSnapshotEntity::getAppCode, "internal-admin")
                        .eq(MangoJobWorkerSnapshotEntity::getEngineType, JobEngineType.MANGO_NATIVE.name())
                        .eq(MangoJobWorkerSnapshotEntity::getWorkerAddress, "http://worker-idempotent:8080"))))
                .singleElement()
                .satisfies(worker -> {
                    assertThat(worker.getEngineWorkerId()).isEqualTo("worker-a");
                    assertThat(worker.getStatus()).isEqualTo(JobWorkerStatus.ONLINE.name());
                });
        assertThat(query("job", () -> workerCapabilityMapper.selectList(
                new LambdaQueryWrapper<MangoJobWorkerCapabilityEntity>()
                        .eq(MangoJobWorkerCapabilityEntity::getTenantId, "tenant-b")
                        .eq(MangoJobWorkerCapabilityEntity::getAppCode, "internal-admin"))))
                .filteredOn(capability -> "syncOrderHandler".equals(capability.getHandlerName())
                        || "otherHandler".equals(capability.getHandlerName()))
                .hasSize(2)
                .anySatisfy(capability -> {
                    assertThat(capability.getHandlerName()).isEqualTo("syncOrderHandler");
                    assertThat(capability.getEnabled()).isZero();
                })
                .anySatisfy(capability -> {
                    assertThat(capability.getHandlerName()).isEqualTo("otherHandler");
                    assertThat(capability.getEnabled()).isEqualTo(1);
                });
    }

    @Test
    void workerRegistry_shouldKeepOneWorkerWhenSameInstanceRegistersConcurrently() throws Exception {
        MangoContextHolder.set(MangoContextSnapshot.request("req-worker-register-race", "trace-worker-register-race",
                        "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(223L, "tenant-b", "job-admin", "test", "user", "tenant", 223L, "internal-admin"));

        int workers = 6;
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<? extends Future<?>> futures = IntStream.range(0, workers)
                    .mapToObj(index -> executor.submit(() -> {
                        MangoContextHolder.set(MangoContextSnapshot.request("req-worker-register-race-" + index,
                                        "trace-worker-register-race-" + index, "tenant-b", "internal-admin",
                                        "127.0.0.1")
                                .withSecurity(223L, "tenant-b", "job-admin", "test", "user",
                                        "tenant", 223L, "internal-admin"));
                        ready.countDown();
                        try {
                            if (!start.await(5, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("worker register race timeout");
                            }
                            registerNativeWorker("http://worker-race:8080", "syncOrderHandler");
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("worker register interrupted", ex);
                        } finally {
                            MangoContextHolder.clear();
                        }
                        return null;
                    }))
                    .toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(query("job", () -> workerSnapshotMapper.selectList(
                new LambdaQueryWrapper<MangoJobWorkerSnapshotEntity>()
                        .eq(MangoJobWorkerSnapshotEntity::getTenantId, "tenant-b")
                        .eq(MangoJobWorkerSnapshotEntity::getServiceCode, "internal-admin")
                        .eq(MangoJobWorkerSnapshotEntity::getWorkerGroup, "internal-admin")
                        .eq(MangoJobWorkerSnapshotEntity::getWorkerAddress, "http://worker-race:8080"))))
                .hasSize(1);
        assertThat(query("job", () -> workerCapabilityMapper.selectList(
                new LambdaQueryWrapper<MangoJobWorkerCapabilityEntity>()
                        .eq(MangoJobWorkerCapabilityEntity::getTenantId, "tenant-b")
                        .eq(MangoJobWorkerCapabilityEntity::getServiceCode, "internal-admin")
                        .eq(MangoJobWorkerCapabilityEntity::getWorkerGroup, "internal-admin")
                        .eq(MangoJobWorkerCapabilityEntity::getHandlerName, "syncOrderHandler"))))
                .singleElement()
                .satisfies(capability -> assertThat(capability.getJobCode()).isEmpty());
    }

    @Test
    void workerRegistry_shouldAllowSameAddressForDifferentWorkerGroupsAndDispatchByOwner() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-worker-owner-same-address", "trace-worker-owner-same-address",
                        "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(224L, "tenant-b", "job-admin", "test", "user", "tenant", 224L, "internal-admin"));

        nativeProperties.setEmbeddedWorkerEnabled(false);
        try {
            SaveMangoJobDefinitionCommand command = definitionCommand("service-a-same-address-job");
            command.setOwnerService("service-a");
            command.setWorkerGroup("service-a");
            Long jobId = jobDefinitionService.createDefinition(command);
            UpdateMangoJobDefinitionStatusCommand statusCommand = new UpdateMangoJobDefinitionStatusCommand();
            statusCommand.setId(jobId);
            statusCommand.setStatus(JobDefinitionStatus.ENABLED.name());
            jobDefinitionService.updateDefinitionStatus(statusCommand);

            registerNativeWorker("http://shared-worker:8080", "syncOrderHandler", "service-a", "service-a");
            registerNativeWorker("http://shared-worker:8080", "syncOrderHandler", "service-b", "service-b");

            assertThat(query("job", () -> workerSnapshotMapper.selectList(
                    new LambdaQueryWrapper<MangoJobWorkerSnapshotEntity>()
                            .eq(MangoJobWorkerSnapshotEntity::getTenantId, "tenant-b")
                            .eq(MangoJobWorkerSnapshotEntity::getWorkerAddress, "http://shared-worker:8080"))))
                    .hasSize(2)
                    .extracting(MangoJobWorkerSnapshotEntity::getServiceCode)
                    .containsExactlyInAnyOrder("service-a", "service-b");

            TriggerMangoJobCommand triggerCommand = new TriggerMangoJobCommand();
            triggerCommand.setJobId(jobId);
            triggerCommand.setTriggerBatchNo("service-a-same-address-batch");
            Long instanceId = jobDefinitionService.triggerDefinition(triggerCommand);

            assertThat(jobQueryService.pageInstances(new MangoJobInstancePageQuery()).getList())
                    .singleElement()
                    .satisfies(instance -> {
                        assertThat(instance.getId()).isEqualTo(instanceId);
                        assertThat(instance.getStatus()).isEqualTo("SUCCESS");
                        assertThat(instance.getWorkerAddress()).isEqualTo("http://shared-worker:8080");
                    });
            MangoJobWorkerPageQuery workerPageQuery = new MangoJobWorkerPageQuery();
            workerPageQuery.setServiceCode("service-a");
            workerPageQuery.setWorkerGroup("service-a");
            assertThat(jobQueryService.pageWorkers(workerPageQuery).getList())
                    .singleElement()
                    .satisfies(worker -> {
                        assertThat(worker.getServiceCode()).isEqualTo("service-a");
                        assertThat(worker.getWorkerGroup()).isEqualTo("service-a");
                        assertThat(worker.getWorkerAddress()).isEqualTo("http://shared-worker:8080");
                    });
            assertThat(query("job", () -> workerSnapshotMapper.selectList(
                    new LambdaQueryWrapper<MangoJobWorkerSnapshotEntity>()
                            .eq(MangoJobWorkerSnapshotEntity::getTenantId, "tenant-b")
                            .eq(MangoJobWorkerSnapshotEntity::getServiceCode, "service-a")
                            .eq(MangoJobWorkerSnapshotEntity::getWorkerGroup, "service-a")
                            .eq(MangoJobWorkerSnapshotEntity::getWorkerAddress, "http://shared-worker:8080"))))
                    .singleElement()
                    .satisfies(worker -> assertThat(worker.getTransportType()).isEqualTo(JobTransportType.HTTP_INTERNAL.name()));
        } finally {
            nativeProperties.setEmbeddedWorkerEnabled(true);
        }
    }

    @Test
    void workerRegistry_shouldSupportManualCreateStatusGovernanceAndHeartbeatProtection() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-worker-governance", "trace-worker-governance",
                        "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(214L, "tenant-b", "job-admin", "test", "user", "tenant", 214L, "internal-admin"));

        CreateMangoJobWorkerCommand createCommand = new CreateMangoJobWorkerCommand();
        createCommand.setAppCode("internal-admin");
        createCommand.setWorkerAddress("http://worker-manual:8080");
        createCommand.setTransportType(JobTransportType.HTTP_INTERNAL);
        createCommand.setWorkerInstanceId("worker-manual-1");
        MangoJobHandlerVO handler = new MangoJobHandlerVO();
        handler.setAppCode("internal-admin");
        handler.setHandlerName("syncOrderHandler");
        createCommand.getHandlers().add(handler);

        Long workerId = workerRegistryService.createWorker(createCommand);
        assertThat(query("job", () -> workerSnapshotMapper.selectById(workerId)))
                .satisfies(worker -> {
                    assertThat(worker.getTenantId()).isEqualTo("tenant-b");
                    assertThat(worker.getStatus()).isEqualTo(JobWorkerStatus.ONLINE.name());
                    assertThat(worker.getWorkerAddress()).isEqualTo("http://worker-manual:8080");
                });

        UpdateMangoJobWorkerStatusCommand statusCommand = new UpdateMangoJobWorkerStatusCommand();
        statusCommand.setId(workerId);
        statusCommand.setStatus(JobWorkerStatus.DISABLED);
        assertThat(workerRegistryService.updateWorkerStatus(statusCommand)).isTrue();

        registerNativeWorker("http://worker-manual:8080", "syncOrderHandler");
        assertThat(query("job", () -> workerSnapshotMapper.selectById(workerId)).getStatus())
                .isEqualTo(JobWorkerStatus.DISABLED.name());

        statusCommand.setStatus(JobWorkerStatus.ONLINE);
        assertThat(workerRegistryService.updateWorkerStatus(statusCommand)).isTrue();
        assertThat(query("job", () -> workerSnapshotMapper.selectById(workerId)).getStatus())
                .isEqualTo(JobWorkerStatus.ONLINE.name());
    }

    @Test
    void nativeRuntime_shouldNotDispatchToManuallyDisabledEmbeddedWorker() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-worker-disable-dispatch", "trace-worker-disable-dispatch",
                        "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(215L, "tenant-b", "job-admin", "test", "user", "tenant", 215L, "internal-admin"));

        Long jobId = jobDefinitionService.createDefinition(definitionCommand("sync-disabled-worker"));
        UpdateMangoJobDefinitionStatusCommand statusCommand = new UpdateMangoJobDefinitionStatusCommand();
        statusCommand.setId(jobId);
        statusCommand.setStatus(JobDefinitionStatus.ENABLED.name());
        jobDefinitionService.updateDefinitionStatus(statusCommand);

        MangoJobWorkerSnapshotEntity worker = query("job", () -> workerSnapshotMapper.selectList(
                        new LambdaQueryWrapper<MangoJobWorkerSnapshotEntity>()
                                .eq(MangoJobWorkerSnapshotEntity::getTenantId, "tenant-b")
                                .eq(MangoJobWorkerSnapshotEntity::getAppCode, "internal-admin")
                                .eq(MangoJobWorkerSnapshotEntity::getEngineType, JobEngineType.MANGO_NATIVE.name())))
                .get(0);
        UpdateMangoJobWorkerStatusCommand workerStatus = new UpdateMangoJobWorkerStatusCommand();
        workerStatus.setId(worker.getId());
        workerStatus.setStatus(JobWorkerStatus.DISABLED);
        workerRegistryService.updateWorkerStatus(workerStatus);

        TriggerMangoJobCommand triggerCommand = new TriggerMangoJobCommand();
        triggerCommand.setJobId(jobId);
        triggerCommand.setTriggerBatchNo("disabled-worker-batch");
        assertThatThrownBy(() -> jobDefinitionService.triggerDefinition(triggerCommand))
                .hasMessageContaining("未找到可执行任务的在线 Worker：internal-admin/internal-admin/"
                        + "internal-admin/syncOrderHandler");

        MangoJobInstancePageQuery query = new MangoJobInstancePageQuery();
        query.setJobId(jobId);
        assertThat(jobQueryService.pageInstances(query).getList())
                .singleElement()
                .satisfies(instance -> {
                    assertThat(instance.getStatus()).isEqualTo("FAILED");
                    assertThat(instance.getErrorSummary()).isEqualTo("未找到可执行任务的在线 Worker："
                            + "internal-admin/internal-admin/internal-admin/syncOrderHandler");
                });
    }

    @Test
    void nativeRuntime_shouldNotDispatchServiceAJobToServiceBWorker() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-worker-owner-guard", "trace-worker-owner-guard",
                        "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(221L, "tenant-b", "job-admin", "test", "user", "tenant", 221L, "internal-admin"));

        nativeProperties.setEmbeddedWorkerEnabled(false);
        try {
            SaveMangoJobDefinitionCommand command = definitionCommand("service-a-owned-job");
            command.setOwnerService("service-a");
            command.setWorkerGroup("service-a");
            command.setHandlerName("syncOrderHandler");
            Long jobId = jobDefinitionService.createDefinition(command);
            UpdateMangoJobDefinitionStatusCommand statusCommand = new UpdateMangoJobDefinitionStatusCommand();
            statusCommand.setId(jobId);
            statusCommand.setStatus(JobDefinitionStatus.ENABLED.name());
            jobDefinitionService.updateDefinitionStatus(statusCommand);

            registerNativeWorker("http://service-b-worker:8080", "syncOrderHandler", "service-b", "service-b");

            TriggerMangoJobCommand triggerCommand = new TriggerMangoJobCommand();
            triggerCommand.setJobId(jobId);
            triggerCommand.setTriggerBatchNo("service-owner-guard-batch");
            assertThatThrownBy(() -> jobDefinitionService.triggerDefinition(triggerCommand))
                    .hasMessageContaining("未找到可执行任务的 Worker 能力：service-a/service-a/"
                            + "internal-admin/syncOrderHandler/service-a-owned-job");

            MangoJobInstancePageQuery query = new MangoJobInstancePageQuery();
            query.setJobId(jobId);
            assertThat(jobQueryService.pageInstances(query).getList())
                    .singleElement()
                    .satisfies(instance -> {
                        assertThat(instance.getStatus()).isEqualTo("FAILED");
                        assertThat(instance.getWorkerAddress()).isNull();
                        assertThat(instance.getErrorSummary()).contains("service-a/service-a");
                    });
        } finally {
            nativeProperties.setEmbeddedWorkerEnabled(true);
        }
    }

    @Test
    void queryService_shouldFilterInvalidExpireStaleWorkersAndRecoverOnHeartbeat() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-8", "trace-8", "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(207L, "tenant-b", "job-admin", "test", "user", "tenant", 207L, "internal-admin"));

        insertWorker("N/A", "ONLINE", LocalDateTime.now());
        insertWorker("127.0.0.1:27777", "ONLINE", LocalDateTime.now());
        insertWorker("127.0.0.1:28888", "ONLINE", LocalDateTime.now().minusMinutes(11));

        assertThat(jobQueryService.pageWorkers(new MangoJobWorkerPageQuery()).getList())
                .extracting("workerAddress")
                .containsExactly("127.0.0.1:27777", "127.0.0.1:28888");

        MangoJobWorkerPageQuery onlineQuery = new MangoJobWorkerPageQuery();
        onlineQuery.setStatus("ONLINE");
        assertThat(jobQueryService.pageWorkers(onlineQuery).getList())
                .singleElement()
                .satisfies(worker -> {
                    assertThat(worker.getWorkerAddress()).isEqualTo("127.0.0.1:27777");
                    assertThat(worker.getStatus()).isEqualTo("ONLINE");
                });

        MangoJobWorkerPageQuery expiredQuery = new MangoJobWorkerPageQuery();
        expiredQuery.setStatus("EXPIRED");
        assertThat(jobQueryService.pageWorkers(expiredQuery).getList())
                .singleElement()
                .satisfies(worker -> {
                    assertThat(worker.getWorkerAddress()).isEqualTo("127.0.0.1:28888");
                    assertThat(worker.getStatus()).isEqualTo("EXPIRED");
                });

        registerNativeWorker("http://worker-recover:8080", "syncOrderHandler");
        query("job", () -> {
            workerSnapshotMapper.update(null, new LambdaUpdateWrapper<MangoJobWorkerSnapshotEntity>()
                    .eq(MangoJobWorkerSnapshotEntity::getTenantId, "tenant-b")
                    .eq(MangoJobWorkerSnapshotEntity::getWorkerAddress, "http://worker-recover:8080")
                    .set(MangoJobWorkerSnapshotEntity::getStatus, JobWorkerStatus.EXPIRED.name())
                    .set(MangoJobWorkerSnapshotEntity::getLastHeartbeatAt, LocalDateTime.now().minusMinutes(11)));
            return null;
        });
        registerNativeWorker("http://worker-recover:8080", "syncOrderHandler");

        assertThat(query("job", () -> workerSnapshotMapper.selectList(
                new LambdaQueryWrapper<MangoJobWorkerSnapshotEntity>()
                        .eq(MangoJobWorkerSnapshotEntity::getTenantId, "tenant-b")
                        .eq(MangoJobWorkerSnapshotEntity::getWorkerAddress, "http://worker-recover:8080"))))
                .singleElement()
                .satisfies(worker -> {
                    assertThat(worker.getStatus()).isEqualTo(JobWorkerStatus.ONLINE.name());
                    assertThat(worker.getLastHeartbeatAt()).isAfter(LocalDateTime.now().minusMinutes(1));
                });
    }

    @Test
    void definitionService_shouldMarkInstanceFailedWhenEngineTriggerFails() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-7", "trace-7", "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(206L, "tenant-b", "job-admin", "test", "user", "tenant", 206L, "internal-admin"));

        Long jobId = jobDefinitionService.createDefinition(definitionCommand("sync-failed-trigger"));
        UpdateMangoJobDefinitionStatusCommand statusCommand = new UpdateMangoJobDefinitionStatusCommand();
        statusCommand.setId(jobId);
        statusCommand.setStatus(JobDefinitionStatus.ENABLED.name());
        jobDefinitionService.updateDefinitionStatus(statusCommand);

        TriggerMangoJobCommand triggerCommand = new TriggerMangoJobCommand();
        triggerCommand.setJobId(jobId);
        triggerCommand.setTriggerBatchNo("fail-trigger");

        nativeProperties.setEmbeddedWorkerEnabled(false);
        try {
            cleanTable("job", "mango_job_worker_capability");
            cleanTable("job", "mango_job_worker_snapshot");
            assertThatThrownBy(() -> jobDefinitionService.triggerDefinition(triggerCommand))
                    .hasMessageContaining("未找到可执行任务的 Worker 能力：internal-admin/internal-admin/"
                            + "internal-admin/syncOrderHandler/sync-failed-trigger");

            MangoJobInstancePageQuery query = new MangoJobInstancePageQuery();
            query.setJobId(jobId);
            assertThat(jobQueryService.pageInstances(query).getList())
                    .singleElement()
                    .satisfies(instance -> {
                        assertThat(instance.getStatus()).isEqualTo("FAILED");
                        assertThat(instance.getErrorSummary()).isEqualTo("未找到可执行任务的 Worker 能力："
                                + "internal-admin/internal-admin/internal-admin/syncOrderHandler/sync-failed-trigger");
                    });
            assertThat(jobQueryService.pageLogs(new MangoJobLogPageQuery()).getTotal()).isEqualTo(1);
        } finally {
            nativeProperties.setEmbeddedWorkerEnabled(true);
        }
    }

    @Test
    void nativeRuntime_shouldSendNoticeWhenFailedInstanceMatchesEnabledAlarmRule() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-alarm", "trace-alarm",
                        "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(216L, "tenant-b", "job-admin", "test", "user", "tenant", 216L, "internal-admin"));
        noticeApiCapture.lastCommand = null;

        SaveMangoJobDefinitionCommand command = definitionCommand("native-failed-alarm");
        command.setHandlerName("failedOrderHandler");
        Long jobId = jobDefinitionService.createDefinition(command);
        query("job", () -> {
            MangoJobAlarmRuleEntity rule = new MangoJobAlarmRuleEntity();
            rule.setTenantId("tenant-b");
            rule.setJobId(jobId);
            rule.setAppCode("internal-admin");
            rule.setRuleName("失败任务告警");
            rule.setAlarmType("INSTANCE_FAILED");
            rule.setTriggerCondition("{\"status\":\"FAILED\"}");
            rule.setNoticeSceneCode(MangoJobNoticeBizTypes.JOB_INSTANCE_FAILED);
            rule.setNoticeTemplateCode(MangoJobNoticeBizTypes.JOB_INSTANCE_FAILED_SITE_TEMPLATE);
            rule.setNoticeParams("{\"userIds\":[216]}");
            rule.setEnabled(1);
            alarmRuleMapper.insert(rule);
            return null;
        });
        UpdateMangoJobDefinitionStatusCommand statusCommand = new UpdateMangoJobDefinitionStatusCommand();
        statusCommand.setId(jobId);
        statusCommand.setStatus(JobDefinitionStatus.ENABLED.name());
        jobDefinitionService.updateDefinitionStatus(statusCommand);

        TriggerMangoJobCommand triggerCommand = new TriggerMangoJobCommand();
        triggerCommand.setJobId(jobId);
        triggerCommand.setTriggerBatchNo("failed-alarm-batch");
        Long instanceId = jobDefinitionService.triggerDefinition(triggerCommand);

        assertThat(noticeApiCapture.lastCommand).isNotNull()
                .satisfies(notice -> {
                    assertThat(notice.getBizType()).isEqualTo(MangoJobNoticeBizTypes.JOB_INSTANCE_FAILED);
                    assertThat(notice.getBizId()).isEqualTo(String.valueOf(instanceId));
                    assertThat(notice.getUserIds()).containsExactly(216L);
                    assertThat(notice.getIdempotentKey()).startsWith("mango-job:alarm:");
                    assertThat(notice.getParams())
                            .containsEntry("jobCode", "native-failed-alarm")
                            .containsEntry("noticeTemplateCode", MangoJobNoticeBizTypes.JOB_INSTANCE_FAILED_SITE_TEMPLATE)
                            .containsEntry("errorSummary", "handler failed intentionally");
                });
        MangoJobInstancePageQuery query = new MangoJobInstancePageQuery();
        query.setJobId(jobId);
        assertThat(jobQueryService.pageInstances(query).getList())
                .singleElement()
                .satisfies(instance -> {
                    assertThat(instance.getStatus()).isEqualTo("FAILED");
                    assertThat(instance.getErrorSummary()).isEqualTo("handler failed intentionally");
                });
        assertThat(jobQueryService.detailInstanceLog(instanceId).getContent())
                .contains("Job 失败告警已提交到 mango-notice");
    }

    @Test
    void alarmRuleService_shouldManageCrudStatusAndTenantIsolationOnJobDatasource() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-alarm-crud", "trace-alarm-crud",
                        "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(217L, "tenant-b", "job-admin", "test", "user", "tenant", 217L, "internal-admin"));

        Long jobId = jobDefinitionService.createDefinition(definitionCommand("alarm-crud-job"));
        SaveMangoJobAlarmRuleCommand createCommand = alarmRuleCommand("失败告警规则", jobId);

        Long alarmRuleId = alarmRuleService.createAlarmRule(createCommand);

        assertThat(rowCount("primary", "mango_job_alarm_rule")).isZero();
        assertThat(rowCount("job", "mango_job_alarm_rule")).isOne();
        MangoJobAlarmRuleVO detail = alarmRuleService.detailAlarmRule(alarmRuleId);
        assertThat(detail)
                .satisfies(rule -> {
                    assertThat(rule.getTenantId()).isEqualTo("tenant-b");
                    assertThat(rule.getJobId()).isEqualTo(jobId);
                    assertThat(rule.getJobCode()).isEqualTo("alarm-crud-job");
                    assertThat(rule.getJobName()).isEqualTo("Sync alarm-crud-job");
                    assertThat(rule.getAlarmType()).isEqualTo("INSTANCE_FAILED");
                    assertThat(rule.getNoticeSceneCode()).isEqualTo(MangoJobNoticeBizTypes.JOB_INSTANCE_FAILED);
                    assertThat(rule.getNoticeTemplateCode()).isEqualTo(MangoJobNoticeBizTypes.JOB_INSTANCE_FAILED_SITE_TEMPLATE);
                    assertThat(rule.getNoticeParams()).contains("recipientRuleCode");
                    assertThat(rule.getEnabled()).isTrue();
                });

        MangoJobAlarmRulePageQuery pageQuery = new MangoJobAlarmRulePageQuery();
        pageQuery.setKeyword("失败");
        assertThat(alarmRuleService.pageAlarmRules(pageQuery).getList())
                .singleElement()
                .extracting(MangoJobAlarmRuleVO::getId)
                .isEqualTo(alarmRuleId);

        createCommand.setId(alarmRuleId);
        createCommand.setRuleName("失败告警规则已更新");
        createCommand.setNoticeTemplateCode("job.instance.failed.site.v2");
        createCommand.setNoticeParams("{\"recipientRuleCode\":\"jobDuty\",\"userId\":217,\"userIds\":[217,218]}");
        assertThat(alarmRuleService.updateAlarmRule(createCommand)).isTrue();
        assertThat(alarmRuleService.detailAlarmRule(alarmRuleId))
                .satisfies(rule -> {
                    assertThat(rule.getRuleName()).isEqualTo("失败告警规则已更新");
                    assertThat(rule.getNoticeTemplateCode()).isEqualTo("job.instance.failed.site.v2");
                    assertThat(rule.getNoticeParams()).contains("\"userId\":217");
                });

        UpdateMangoJobAlarmRuleStatusCommand statusCommand = new UpdateMangoJobAlarmRuleStatusCommand();
        statusCommand.setId(alarmRuleId);
        statusCommand.setEnabled(false);
        assertThat(alarmRuleService.updateAlarmRuleStatus(statusCommand)).isTrue();
        assertThat(alarmRuleService.detailAlarmRule(alarmRuleId).getEnabled()).isFalse();

        MangoContextHolder.set(MangoContextSnapshot.request("req-alarm-crud-other", "trace-alarm-crud-other",
                        "tenant-c", "internal-admin", "127.0.0.1")
                .withSecurity(218L, "tenant-c", "job-admin", "test", "user", "tenant", 218L, "internal-admin"));
        assertThat(alarmRuleService.pageAlarmRules(new MangoJobAlarmRulePageQuery()).getTotal()).isZero();
        assertThatThrownBy(() -> alarmRuleService.detailAlarmRule(alarmRuleId))
                .hasMessageContaining("告警规则不存在");

        MangoContextHolder.set(MangoContextSnapshot.request("req-alarm-crud-delete", "trace-alarm-crud-delete",
                        "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(217L, "tenant-b", "job-admin", "test", "user", "tenant", 217L, "internal-admin"));
        assertThat(alarmRuleService.deleteAlarmRule(alarmRuleId)).isTrue();
        assertThat(alarmRuleService.pageAlarmRules(new MangoJobAlarmRulePageQuery()).getTotal()).isZero();
    }

    @Test
    void alarmRuleService_shouldRejectInvalidJsonAndMismatchedJobScope() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-alarm-invalid", "trace-alarm-invalid",
                        "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(219L, "tenant-b", "job-admin", "test", "user", "tenant", 219L, "internal-admin"));

        Long jobId = jobDefinitionService.createDefinition(definitionCommand("alarm-invalid-job"));
        SaveMangoJobAlarmRuleCommand invalidJsonCommand = alarmRuleCommand("非法 JSON 告警", jobId);
        invalidJsonCommand.setNoticeParams("{bad-json");

        assertThatThrownBy(() -> alarmRuleService.createAlarmRule(invalidJsonCommand))
                .hasMessageContaining("通知参数 JSON 不合法");

        SaveMangoJobAlarmRuleCommand mismatchCommand = alarmRuleCommand("应用不匹配告警", jobId);
        mismatchCommand.setAppCode("other-admin");
        assertThatThrownBy(() -> alarmRuleService.createAlarmRule(mismatchCommand))
                .hasMessageContaining("告警规则所属应用必须与任务所属应用一致");

        MangoContextHolder.set(MangoContextSnapshot.request("req-alarm-invalid-other", "trace-alarm-invalid-other",
                        "tenant-c", "internal-admin", "127.0.0.1")
                .withSecurity(220L, "tenant-c", "job-admin", "test", "user", "tenant", 220L, "internal-admin"));
        SaveMangoJobAlarmRuleCommand crossTenantCommand = alarmRuleCommand("跨租户任务告警", jobId);
        assertThatThrownBy(() -> alarmRuleService.createAlarmRule(crossTenantCommand))
                .hasMessageContaining("任务不存在");
    }

    @Test
    void definitionService_shouldRejectTooSmallFixedRateExpression() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-4", "trace-4", "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(203L, "tenant-b", "job-admin", "test", "user", "tenant", 203L, "internal-admin"));

        SaveMangoJobDefinitionCommand command = definitionCommand("sync-fixed-rate");
        command.setScheduleType(JobScheduleType.FIXED_RATE.name());
        command.setScheduleExpression("300");

        assertThatThrownBy(() -> jobDefinitionService.createDefinition(command))
                .hasMessageContaining("固定频率调度表达式单位为毫秒，最小值为1000");
        assertThat(rowCount("job", "mango_job_definition")).isZero();
    }

    @Test
    void definitionService_shouldRejectTooLargeFixedRateExpression() {
        MangoContextHolder.set(MangoContextSnapshot.request("req-5", "trace-5", "tenant-b", "internal-admin", "127.0.0.1")
                .withSecurity(204L, "tenant-b", "job-admin", "test", "user", "tenant", 204L, "internal-admin"));

        SaveMangoJobDefinitionCommand command = definitionCommand("sync-fixed-rate-large");
        command.setScheduleType(JobScheduleType.FIXED_RATE.name());
        command.setScheduleExpression("120000");

        assertThatThrownBy(() -> jobDefinitionService.createDefinition(command))
                .hasMessageContaining("固定频率调度表达式单位为毫秒，必须小于120000");
        assertThat(rowCount("job", "mango_job_definition")).isZero();
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

    private boolean columnExists(String datasource, String tableName, String columnName) {
        try {
            return query(datasource, () -> jdbcTemplate().queryForObject("""
                    SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE LOWER(TABLE_NAME) = LOWER(?)
                      AND LOWER(COLUMN_NAME) = LOWER(?)
                    """, Integer.class, tableName, columnName) > 0);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean constraintOrIndexExists(String datasource, String tableName, String name) {
        return query(datasource, () -> {
            Integer constraintCount = jdbcTemplate().queryForObject("""
                    SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                    WHERE LOWER(TABLE_NAME) = LOWER(?)
                      AND LOWER(CONSTRAINT_NAME) = LOWER(?)
                    """, Integer.class, tableName, name);
            if (constraintCount != null && constraintCount > 0) {
                return true;
            }
            return jdbcTemplate().queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.INDEXES
                WHERE LOWER(TABLE_NAME) = LOWER(?)
                  AND LOWER(INDEX_NAME) = LOWER(?)
                """, Integer.class, tableName, name) > 0;
        });
    }

    private int rowCount(String datasource, String tableName) {
        if (!tableExists(datasource, tableName)) {
            return 0;
        }
        return query(datasource, () -> jdbcTemplate().queryForObject("SELECT COUNT(*) FROM " + tableName,
                Integer.class));
    }

    private void cleanDatasource(String datasource) {
        cleanTable(datasource, "mango_job_event");
        cleanTable(datasource, "mango_job_log_chunk");
        cleanTable(datasource, "mango_job_worker_capability");
        cleanTable(datasource, "mango_job_attempt");
        cleanTable(datasource, "mango_job_schedule_cursor");
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
        command.setOwnerService("internal-admin");
        command.setWorkerGroup("internal-admin");
        command.setJobCode(jobCode);
        command.setJobName("Sync " + jobCode);
        command.setJobType(JobType.BUILTIN.name());
        command.setScheduleType(JobScheduleType.MANUAL.name());
        command.setHandlerName("syncOrderHandler");
        command.setEngineType(JobEngineType.MANGO_NATIVE.name());
        return command;
    }

    private SaveMangoJobAlarmRuleCommand alarmRuleCommand(String ruleName, Long jobId) {
        SaveMangoJobAlarmRuleCommand command = new SaveMangoJobAlarmRuleCommand();
        command.setAppCode("internal-admin");
        command.setJobId(jobId);
        command.setRuleName(ruleName);
        command.setAlarmType("INSTANCE_FAILED");
        command.setTriggerCondition("{\"status\":\"FAILED\"}");
        command.setNoticeSceneCode(MangoJobNoticeBizTypes.JOB_INSTANCE_FAILED);
        command.setNoticeTemplateCode(MangoJobNoticeBizTypes.JOB_INSTANCE_FAILED_SITE_TEMPLATE);
        command.setNoticeParams("{\"recipientRuleCode\":\"jobDuty\",\"userIds\":[217]}");
        command.setEnabled(true);
        return command;
    }

    private void insertWorker(String workerAddress, String status, LocalDateTime lastHeartbeatAt) {
        MangoJobWorkerSnapshotEntity worker = new MangoJobWorkerSnapshotEntity();
        worker.setTenantId("tenant-b");
        worker.setAppCode("internal-admin");
        worker.setServiceCode("internal-admin");
        worker.setWorkerGroup("internal-admin");
        worker.setEngineType(JobEngineType.MANGO_NATIVE.name());
        worker.setEngineWorkerId(workerAddress);
        worker.setInstanceId(workerAddress);
        worker.setWorkerAddress(workerAddress);
        worker.setRuntimeAddress(workerAddress);
        worker.setTransportType(workerAddress.startsWith("in-memory://")
                ? JobTransportType.IN_MEMORY.name() : JobTransportType.HTTP_INTERNAL.name());
        worker.setRegisterSource(JobWorkerRegisterSource.REMOTE_AUTO.name());
        worker.setStatus(status);
        worker.setLastHeartbeatAt(lastHeartbeatAt);
        query("job", () -> {
            workerSnapshotMapper.insert(worker);
            return null;
        });
    }

    private void registerNativeWorker(String workerAddress, String handlerName) {
        registerNativeWorker(workerAddress, handlerName, "internal-admin", "internal-admin");
    }

    private void registerNativeWorker(String workerAddress,
                                      String handlerName,
                                      String serviceCode,
                                      String workerGroup) {
        RegisterMangoJobWorkerCommand command = new RegisterMangoJobWorkerCommand();
        command.setTenantId("tenant-b");
        command.setAppCode("internal-admin");
        command.setServiceCode(serviceCode);
        command.setWorkerGroup(workerGroup);
        command.setWorkerAddress(workerAddress);
        command.setRuntimeAddress(workerAddress);
        command.setTransportType(JobTransportType.HTTP_INTERNAL);
        command.setRegisterSource(JobWorkerRegisterSource.REMOTE_AUTO);
        command.setWorkerInstanceId("worker-a");
        MangoJobHandlerVO handler = new MangoJobHandlerVO();
        handler.setAppCode("internal-admin");
        handler.setServiceCode(serviceCode);
        handler.setWorkerGroup(workerGroup);
        handler.setHandlerName(handlerName);
        command.getHandlers().add(handler);
        workerRegistryService.registerWorker(command);
    }

    private void registerEmbeddedWorker(String workerAddress,
                                        String instanceId,
                                        String handlerName,
                                        String serviceCode,
                                        String workerGroup) {
        RegisterMangoJobWorkerCommand command = new RegisterMangoJobWorkerCommand();
        command.setTenantId("tenant-b");
        command.setAppCode("internal-admin");
        command.setServiceCode(serviceCode);
        command.setWorkerGroup(workerGroup);
        command.setWorkerAddress(workerAddress);
        command.setRuntimeAddress(workerAddress);
        command.setTransportType(JobTransportType.IN_MEMORY);
        command.setRegisterSource(JobWorkerRegisterSource.EMBEDDED_AUTO);
        command.setWorkerInstanceId(instanceId);
        MangoJobHandlerVO handler = new MangoJobHandlerVO();
        handler.setAppCode("internal-admin");
        handler.setServiceCode(serviceCode);
        handler.setWorkerGroup(workerGroup);
        handler.setHandlerName(handlerName);
        command.getHandlers().add(handler);
        workerRegistryService.registerWorker(command);
    }

    private void runTickWithContext(MangoContextSnapshot snapshot, CountDownLatch ready, CountDownLatch start) {
        MangoContextHolder.set(snapshot);
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("tick start latch timeout");
            }
            nativeJobRuntime.tick();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("tick interrupted", ex);
        } finally {
            MangoContextHolder.clear();
        }
    }

    @Autowired
    private TestHttpInternalTransport httpInternalTransport;

    @Autowired
    private TestNoticeApiCapture noticeApiCapture;

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @MapperScan(basePackageClasses = MangoJobDefinitionMapper.class)
    static class TestApplication {

        @Bean
        @Order(Ordered.LOWEST_PRECEDENCE)
        TestHttpInternalTransport testHttpInternalTransport() {
            return new TestHttpInternalTransport();
        }

        @Bean
        TestNoticeApiCapture testNoticeApiCapture() {
            return new TestNoticeApiCapture();
        }

        @Bean
        NoticeApi noticeApi(TestNoticeApiCapture capture) {
            return (NoticeApi) Proxy.newProxyInstance(NoticeApi.class.getClassLoader(),
                    new Class<?>[]{NoticeApi.class},
                    (proxy, method, args) -> {
                        if ("send".equals(method.getName())) {
                            capture.lastCommand = (SendNoticeCommand) args[0];
                            return R.ok(new NoticeSendResultVO(1, 0));
                        }
                        throw new IllegalStateException("Unexpected NoticeApi method: " + method.getName());
                    });
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
                    System.out.println("syncOrderHandler System.out " + context.getParameter());
                    org.slf4j.LoggerFactory.getLogger("syncOrderHandler")
                            .info("syncOrderHandler logger {}", context.getParameter());
                    return MangoJobHandleResult.success("ok");
                }
            };
        }

        @Bean
        MangoJobHandler failedOrderHandler() {
            return new MangoJobHandler() {
                @Override
                public String handlerName() {
                    return "failedOrderHandler";
                }

                @Override
                public MangoJobHandleResult handle(MangoJobHandleContext context) {
                    return MangoJobHandleResult.failed("handler failed intentionally");
                }
            };
        }

    }

    static class TestNoticeApiCapture {

        private SendNoticeCommand lastCommand;
    }

    static class TestHttpInternalTransport implements IMangoJobWorkerTransport {

        private String lastWorkerAddress;

        private String lastParameter;

        @Override
        public JobTransportType transportType() {
            return JobTransportType.HTTP_INTERNAL;
        }

        @Override
        public MangoJobWorkerExecuteResultVO execute(MangoJobWorkerDispatchRequest request) {
            lastWorkerAddress = request.getWorkerAddress();
            lastParameter = request.getCommand().getParameter();
            MangoJobWorkerExecuteResultVO result = new MangoJobWorkerExecuteResultVO();
            result.setStatus(io.mango.job.api.enums.JobHandleStatus.SUCCESS);
            result.setMessage("remote-ok");
            result.setWorkerAddress(lastWorkerAddress);
            io.mango.job.api.vo.MangoJobWorkerExecutionLogVO log =
                    new io.mango.job.api.vo.MangoJobWorkerExecutionLogVO();
            log.setLevel("INFO");
            log.setLoggerName("System.out");
            log.setContent("remote worker stdout " + lastParameter);
            result.getLogs().add(log);
            return result;
        }
    }

}
