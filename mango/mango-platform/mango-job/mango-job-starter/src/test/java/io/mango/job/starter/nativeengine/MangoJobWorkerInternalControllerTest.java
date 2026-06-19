package io.mango.job.starter.nativeengine;

import io.mango.common.result.R;
import io.mango.job.api.command.MangoJobWorkerExecuteCommand;
import io.mango.job.api.enums.JobHandleStatus;
import io.mango.job.api.enums.JobTriggerType;
import io.mango.job.api.handler.MangoJobHandleContext;
import io.mango.job.api.handler.MangoJobHandleResult;
import io.mango.job.api.handler.MangoJobHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = MangoJobWorkerInternalControllerTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "mango.job.enabled=true",
                "mango.job.native.embedded-worker-enabled=false",
                "mango.job.native.scheduler-enabled=false",
                "mango.job.native.worker-address=http://127.0.0.1:19090",
                "mango.web.inner.secret=test-secret",
                "mango.persistence.flyway.enabled=false",
                "mango.persistence.schema-validation.enabled=false"
        }
)
class MangoJobWorkerInternalControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void executeShouldRunJavaHandlerInRemoteWorkerProcessAndReturnCapturedLogs() {
        MangoJobWorkerExecuteCommand command = new MangoJobWorkerExecuteCommand();
        command.setTenantId("tenant-remote");
        command.setAppCode("remote-worker-app");
        command.setOwnerService("remote-worker-app");
        command.setWorkerGroup("remote-worker-app");
        command.setJobCode("remote-worker-probe");
        command.setHandlerName("remoteProbeHandler");
        command.setInstanceId(90001L);
        command.setOperatorId(1001L);
        command.setTriggerType(JobTriggerType.MANUAL);
        command.setTriggerBatchNo("remote-batch-001");
        command.setTraceId("remote-trace-001");
        command.setParameter("{\"scene\":\"remote-http\"}");

        ResponseEntity<R> response = postInternal(command, "success");

        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("status=%s body=%s", response.getStatusCode(), response.getBody())
                .isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        Object data = response.getBody().getData();
        assertThat(data).isInstanceOf(java.util.Map.class);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> result = (java.util.Map<String, Object>) data;
        assertThat(result.get("status")).isEqualTo(JobHandleStatus.SUCCESS.name());
        assertThat(result.get("message")).isEqualTo("remote-ok");
        assertThat(result.get("workerAddress")).asString().startsWith("http://127.0.0.1:");
        assertThat(result.get("logs").toString()).contains("remoteProbeHandler System.out");
        assertThat(result.get("logs").toString()).contains("remoteProbeHandler logger");
        assertThat(result.get("logs").toString()).contains("remote-http");
    }

    @Test
    void executeShouldRejectMismatchedWorkerOwnershipBeforeBusinessCodeRuns() {
        TestApplication.REMOTE_PROBE_EXECUTIONS.set(0);
        MangoJobWorkerExecuteCommand command = new MangoJobWorkerExecuteCommand();
        command.setTenantId("tenant-remote");
        command.setAppCode("remote-worker-app");
        command.setOwnerService("service-b");
        command.setWorkerGroup("service-b");
        command.setJobCode("remote-worker-probe");
        command.setHandlerName("remoteProbeHandler");
        command.setInstanceId(90002L);
        command.setOperatorId(1001L);
        command.setTriggerType(JobTriggerType.MANUAL);
        command.setTriggerBatchNo("remote-batch-002");
        command.setTraceId("remote-trace-002");
        command.setParameter("{\"scene\":\"wrong-worker\"}");

        ResponseEntity<R> response = postInternal(command, "mismatch");

        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("status=%s body=%s", response.getStatusCode(), response.getBody())
                .isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        Object data = response.getBody().getData();
        assertThat(data).isInstanceOf(java.util.Map.class);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> result = (java.util.Map<String, Object>) data;
        assertThat(result.get("status")).isEqualTo(JobHandleStatus.FAILED.name());
        assertThat(TestApplication.REMOTE_PROBE_EXECUTIONS).hasValue(0);
    }

    private ResponseEntity<R> postInternal(MangoJobWorkerExecuteCommand command, String nonceSuffix) {
        String path = "/job/internal/workers/execute";
        long timestamp = System.currentTimeMillis();
        String nonce = "job-worker-" + nonceSuffix + "-" + timestamp;
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Internal-Call", "true");
        headers.add("X-Internal-Timestamp", Long.toString(timestamp));
        headers.add("X-Internal-Nonce", nonce);
        headers.add("X-Internal-Signature", hmacSha256(timestamp + ":" + nonce + ":POST:" + path + ":"));
        return restTemplate.postForEntity("http://127.0.0.1:" + port + path,
                new HttpEntity<>(command, headers), R.class);
    }

    private String hmacSha256(String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    "test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign internal request", ex);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class})
    static class TestApplication {

        static final AtomicInteger REMOTE_PROBE_EXECUTIONS = new AtomicInteger();

        @Bean
        MangoJobHandler remoteProbeHandler() {
            return new MangoJobHandler() {
                @Override
                public String appCode() {
                    return "remote-worker-app";
                }

                @Override
                public String serviceCode() {
                    return "remote-worker-app";
                }

                @Override
                public String workerGroup() {
                    return "remote-worker-app";
                }

                @Override
                public String handlerName() {
                    return "remoteProbeHandler";
                }

                @Override
                public MangoJobHandleResult handle(MangoJobHandleContext context) {
                    REMOTE_PROBE_EXECUTIONS.incrementAndGet();
                    System.out.println("remoteProbeHandler System.out " + context.getParameter());
                    org.slf4j.LoggerFactory.getLogger("remoteProbeHandler")
                            .info("remoteProbeHandler logger {}", context.getParameter());
                    return MangoJobHandleResult.success("remote-ok");
                }
            };
        }
    }
}
