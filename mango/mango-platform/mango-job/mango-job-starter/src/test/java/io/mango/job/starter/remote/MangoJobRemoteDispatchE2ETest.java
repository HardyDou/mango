package io.mango.job.starter.remote;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.infra.persistence.starter.datasource.PersistenceDataSourceContext;
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
import io.mango.job.api.query.MangoJobInstancePageQuery;
import io.mango.job.api.vo.MangoJobLogDetailVO;
import io.mango.job.core.entity.MangoJobWorkerSnapshotEntity;
import io.mango.job.core.mapper.MangoJobWorkerSnapshotMapper;
import io.mango.job.core.service.IMangoJobDefinitionService;
import io.mango.job.core.service.IMangoJobQueryService;
import io.mango.job.starter.JobAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MangoJobRemoteDispatchE2ETest {

    @Test
    void jobCenterShouldDispatchNativeJobToRemoteWorkerOverHttpInternal() {
        try (ConfigurableApplicationContext jobCenter = startJobCenter()) {
            int jobCenterPort = port(jobCenter);
            int workerPort = freePort();
            try (ConfigurableApplicationContext worker = startWorker(jobCenterPort, workerPort)) {
                waitUntilRegistered(jobCenter, workerPort);
                MangoContextSnapshot snapshot = MangoContextSnapshot
                        .request("remote-e2e-request", "remote-e2e-trace", "tenant-remote-e2e",
                                "remote-worker-app", "127.0.0.1")
                        .withSecurity(9001L, "tenant-remote-e2e", "job-admin", "test",
                                "user", "tenant", 9001L, "remote-worker-app");
                MangoContextHolder.set(snapshot);
                try {
                    Long jobId = createNativeJob(jobCenter);
                    enableJob(jobCenter, jobId);
                    Long instanceId = triggerJob(jobCenter, jobId);
                    assertRemoteExecution(jobCenter, instanceId, workerPort);
                } finally {
                    MangoContextHolder.clear();
                }
            }
        }
    }

    private ConfigurableApplicationContext startJobCenter() {
        return new SpringApplicationBuilder(JobCenterApplication.class)
                .web(WebApplicationType.SERVLET)
                .properties(commonProperties())
                .properties(Map.of(
                        "server.port", "0",
                        "mango.job.enabled", "true",
                        "mango.job.native.embedded-worker-enabled", "false",
                        "mango.job.native.scheduler-enabled", "false"
                ))
                .run();
    }

    private ConfigurableApplicationContext startWorker(int jobCenterPort, int workerPort) {
        return new SpringApplicationBuilder(WorkerApplication.class)
                .web(WebApplicationType.SERVLET)
                .properties(Map.of(
                        "server.port", String.valueOf(workerPort),
                        "mango.job.native.scheduler-tenant-id", "tenant-remote-e2e",
                        "mango.job.native.job-center-address", "http://127.0.0.1:" + jobCenterPort,
                        "mango.job.native.worker-address", "http://127.0.0.1:" + workerPort,
                        "mango.job.native.worker-heartbeat-interval-millis", "600000",
                        "mango.persistence.flyway.enabled", "false",
                        "mango.persistence.schema-validation.enabled", "false"
                ))
                .run();
    }

    private int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException ex) {
            throw new IllegalStateException("failed to allocate worker port", ex);
        }
    }

    private Map<String, Object> commonProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("mango.persistence.datasources.primary.primary", "true");
        properties.put("mango.persistence.datasources.primary.url",
                "jdbc:h2:mem:mango_job_remote_e2e_primary;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        properties.put("mango.persistence.datasources.primary.driver-class-name", "org.h2.Driver");
        properties.put("mango.persistence.datasources.primary.username", "sa");
        properties.put("mango.persistence.datasources.primary.password", "");
        properties.put("mango.persistence.datasources.job.url",
                "jdbc:h2:mem:mango_job_remote_e2e_job;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        properties.put("mango.persistence.datasources.job.driver-class-name", "org.h2.Driver");
        properties.put("mango.persistence.datasources.job.username", "sa");
        properties.put("mango.persistence.datasources.job.password", "");
        properties.put("mango.persistence.modules.mango-job.datasource", "job");
        properties.put("mango.persistence.flyway.enabled", "true");
        properties.put("mango.persistence.mybatis-plus.tenant.enabled", "false");
        properties.put("mango.persistence.schema-validation.enabled", "false");
        return properties;
    }

    private int port(ConfigurableApplicationContext context) {
        return ((WebServerApplicationContext) context).getWebServer().getPort();
    }

    private void waitUntilRegistered(ConfigurableApplicationContext jobCenter, int workerPort) {
        MangoJobWorkerSnapshotMapper mapper = jobCenter.getBean(MangoJobWorkerSnapshotMapper.class);
        Instant deadline = Instant.now().plus(Duration.ofSeconds(20));
        while (Instant.now().isBefore(deadline)) {
            MangoJobWorkerSnapshotEntity worker;
            try (PersistenceDataSourceContext.Scope ignored = PersistenceDataSourceContext.use("job")) {
                worker = mapper.selectOne(new LambdaQueryWrapper<MangoJobWorkerSnapshotEntity>()
                        .eq(MangoJobWorkerSnapshotEntity::getTenantId, "tenant-remote-e2e")
                        .eq(MangoJobWorkerSnapshotEntity::getAppCode, "remote-worker-app")
                        .like(MangoJobWorkerSnapshotEntity::getWorkerAddress, ":" + workerPort)
                        .last("limit 1"));
            }
            if (worker != null) {
                return;
            }
            sleep();
        }
        throw new AssertionError("remote worker was not registered in JobCenter");
    }

    private Long createNativeJob(ConfigurableApplicationContext jobCenter) {
        SaveMangoJobDefinitionCommand command = new SaveMangoJobDefinitionCommand();
        command.setAppCode("remote-worker-app");
        command.setJobCode("remote-http-native-job");
        command.setJobName("Remote HTTP Native Job");
        command.setJobType(JobType.BUILTIN.name());
        command.setScheduleType(JobScheduleType.MANUAL.name());
        command.setHandlerName("remoteHttpNativeHandler");
        command.setParamValue("{\"scene\":\"jobcenter-http-dispatch\"}");
        command.setEngineType(JobEngineType.MANGO_NATIVE.name());
        return jobCenter.getBean(IMangoJobDefinitionService.class).createDefinition(command);
    }

    private void enableJob(ConfigurableApplicationContext jobCenter, Long jobId) {
        UpdateMangoJobDefinitionStatusCommand command = new UpdateMangoJobDefinitionStatusCommand();
        command.setId(jobId);
        command.setStatus(JobDefinitionStatus.ENABLED.name());
        assertThat(jobCenter.getBean(IMangoJobDefinitionService.class).updateDefinitionStatus(command)).isTrue();
    }

    private Long triggerJob(ConfigurableApplicationContext jobCenter, Long jobId) {
        TriggerMangoJobCommand command = new TriggerMangoJobCommand();
        command.setJobId(jobId);
        command.setTriggerBatchNo("remote-http-e2e-batch");
        command.setParamValue("{\"scene\":\"jobcenter-http-trigger\"}");
        return jobCenter.getBean(IMangoJobDefinitionService.class).triggerDefinition(command);
    }

    private void assertRemoteExecution(ConfigurableApplicationContext jobCenter, Long instanceId, int workerPort) {
        IMangoJobQueryService queryService = jobCenter.getBean(IMangoJobQueryService.class);
        MangoJobInstancePageQuery query = new MangoJobInstancePageQuery();
        query.setTriggerBatchNo("remote-http-e2e-batch");
        assertThat(queryService.pageInstances(query).getList())
                .singleElement()
                .satisfies(instance -> {
                    assertThat(instance.getId()).isEqualTo(instanceId);
                    assertThat(instance.getStatus()).isEqualTo("SUCCESS");
                    assertThat(instance.getEngineType()).isEqualTo(JobEngineType.MANGO_NATIVE.name());
                    assertThat(instance.getWorkerAddress()).contains(":" + workerPort);
                    assertThat(instance.getResultSummary()).isEqualTo("remote-http-ok");
                });
        MangoJobLogDetailVO detail = queryService.detailInstanceLog(instanceId);
        assertThat(detail.getContent()).contains("Job attempt leased by http://127.0.0.1:" + workerPort);
        assertThat(detail.getContent()).contains("remoteHttpNativeHandler System.out");
        assertThat(detail.getContent()).contains("remoteHttpNativeHandler logger");
        assertThat(detail.getContent()).contains("jobcenter-http-trigger");
        assertThat(detail.getContent()).contains("handlerResult=remote-http-ok");
    }

    private void sleep() {
        try {
            Thread.sleep(200L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("wait interrupted", ex);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class JobCenterApplication {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class,
            JobAutoConfiguration.class})
    static class WorkerApplication {

        @Bean
        MangoJobHandler remoteHttpNativeHandler() {
            return new MangoJobHandler() {
                @Override
                public String appCode() {
                    return "remote-worker-app";
                }

                @Override
                public String handlerName() {
                    return "remoteHttpNativeHandler";
                }

                @Override
                public MangoJobHandleResult handle(MangoJobHandleContext context) {
                    System.out.println("remoteHttpNativeHandler System.out " + context.getParameter());
                    org.slf4j.LoggerFactory.getLogger("remoteHttpNativeHandler")
                            .info("remoteHttpNativeHandler logger {}", context.getParameter());
                    return MangoJobHandleResult.success("remote-http-ok");
                }
            };
        }
    }
}
