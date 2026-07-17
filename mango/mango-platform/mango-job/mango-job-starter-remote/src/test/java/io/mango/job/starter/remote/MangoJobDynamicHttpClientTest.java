package io.mango.job.starter.remote;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextHeaders;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.web.util.InternalCallSignature;
import io.mango.job.api.command.MangoJobWorkerExecuteCommand;
import io.mango.job.api.command.RegisterMangoJobWorkerCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MangoJobDynamicHttpClientTest {

    private static final String SECRET = "job-remote-test-secret";
    private static final long TIMESTAMP = 1_750_000_000_000L;
    private static final String NONCE = "job-remote-nonce";

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void registerWorker_contextPath_propagatesContextAndSignsRequest() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MangoJobDynamicHttpClient client = client(builder);
        String path = "/platform/job/internal/workers/register";
        String signature = InternalCallSignature.sign(
                Long.toString(TIMESTAMP), NONCE, "POST", path, "", SECRET);
        MangoContextHolder.set(MangoContextSnapshot.request(
                        "request-job-1", "trace-job-1", "tenant-job-1", "job-worker", "127.0.0.8")
                .withSecurity(101L, 201L, "tenant-job-1", "job-admin", "admin",
                        "USER", "ORGANIZATION", 301L, "job-worker"));
        MangoContextHolder.setToken("Bearer job-token");

        server.expect(once(), request -> {
                    assertThat(request.getURI()).isEqualTo(URI.create("http://127.0.0.1:18621" + path));
                    assertThat(request.getMethod()).isEqualTo(HttpMethod.POST);
                    assertThat(request.getHeaders().getFirst("Authorization")).isEqualTo("Bearer job-token");
                    assertThat(request.getHeaders().getFirst(MangoContextHeaders.REQUEST_ID))
                            .isEqualTo("request-job-1");
                    assertThat(request.getHeaders().getFirst(MangoContextHeaders.TRACE_ID))
                            .isEqualTo("trace-job-1");
                    assertThat(request.getHeaders().getFirst(MangoContextHeaders.TENANT_ID))
                            .isEqualTo("tenant-job-1");
                    assertThat(request.getHeaders().getFirst(MangoContextHeaders.APP_CODE))
                            .isEqualTo("job-worker");
                    assertThat(request.getHeaders().getFirst("X-Internal-Call")).isEqualTo("true");
                    assertThat(request.getHeaders().getFirst("X-Internal-Timestamp"))
                            .isEqualTo(Long.toString(TIMESTAMP));
                    assertThat(request.getHeaders().getFirst("X-Internal-Nonce")).isEqualTo(NONCE);
                    assertThat(request.getHeaders().getFirst("X-Internal-Secret-Version")).isEqualTo("7");
                    assertThat(request.getHeaders().getFirst("X-Internal-Signature")).isEqualTo(signature);
                })
                .andRespond(withSuccess("{\"code\":200,\"data\":1}", MediaType.APPLICATION_JSON));

        var response = client.registerWorker(
                URI.create("http://127.0.0.1:18621/platform"), new RegisterMangoJobWorkerCommand());

        assertThat(response).isNotNull();
        assertThat(response.getData()).isEqualTo(1L);
        server.verify();
    }

    @Test
    void executeWorker_existingJobContextPath_doesNotDuplicateJobSegment() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MangoJobDynamicHttpClient client = client(builder);
        String path = "/platform/_job/workers/execute";

        server.expect(once(), request -> {
                    assertThat(request.getURI()).isEqualTo(URI.create("http://127.0.0.1:18622" + path));
                    assertThat(request.getMethod()).isEqualTo(HttpMethod.POST);
                })
                .andRespond(withSuccess("{\"code\":200,\"data\":null}", MediaType.APPLICATION_JSON));

        var response = client.executeWorker(
                URI.create("http://127.0.0.1:18622/platform/job/"), new MangoJobWorkerExecuteCommand());

        assertThat(response).isNotNull();
        server.verify();
    }

    private MangoJobDynamicHttpClient client(RestClient.Builder builder) {
        return new MangoJobDynamicHttpClient(
                builder.build(), SECRET, 7, () -> TIMESTAMP, () -> NONCE);
    }
}
