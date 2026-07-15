package io.mango.infra.feign.starter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import feign.Feign;
import feign.RequestLine;
import feign.Target;
import feign.codec.StringDecoder;
import io.mango.infra.context.api.MangoContextHeaders;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.module.api.ModuleInfo;
import io.mango.infra.web.util.InternalCallSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("flow")
@Tag("infra-feign")
class FeignOutboundFlowTest {

    private static final String SECRET = "feign-flow-secret";

    private HttpServer server;

    @AfterEach
    void cleanup() {
        MangoContextHolder.clear();
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void outboundCall_runtimeTargetAndContext_reachesSignedHttpEndpoint() throws Exception {
        AtomicReference<CapturedRequest> captured = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/platform/probe", exchange -> captureAndRespond(exchange, captured));
        server.start();

        ModuleTargetFeignInterceptor moduleTarget = new ModuleTargetFeignInterceptor(moduleName ->
                java.util.Optional.of(new ModuleInfo(
                        moduleName,
                        "127.0.0.1:" + server.getAddress().getPort(),
                        "/platform",
                        "/probe",
                        "test")));
        InternalCallFeignInterceptor internalCall = new InternalCallFeignInterceptor();
        ReflectionTestUtils.setField(internalCall, "sharedSecret", SECRET);
        MangoContextHolder.set(new MangoContextSnapshot(
                "request-flow", "trace-flow", "tenant-flow", 11L, 22L,
                "principal-flow", "admin", "USER", "TENANT", 33L,
                "admin-app", "127.0.0.1"));
        MangoContextHolder.setToken("Bearer flow-token");
        ProbeClient client = Feign.builder()
                .requestInterceptor(moduleTarget)
                .requestInterceptor(new FeignRequestInterceptor())
                .requestInterceptor(internalCall)
                .decoder(new StringDecoder())
                .target(new Target.HardCodedTarget<>(
                        ProbeClient.class, "mango-probe", "http://127.0.0.1:1"));

        String response = client.probe();

        CapturedRequest request = captured.get();
        assertThat(response).isEqualTo("ok");
        assertThat(request.path()).isEqualTo("/platform/probe");
        assertThat(request.query()).isEqualTo("tag=2&tag=1");
        assertThat(request.authorization()).isEqualTo("Bearer flow-token");
        assertThat(request.tenantId()).isEqualTo("tenant-flow");
        assertThat(request.memberId()).isEqualTo("22");
        String expected = InternalCallSignature.sign(
                request.timestamp(), request.nonce(), "GET", request.path(),
                InternalCallSignature.canonicalizeRawQuery(request.query()), SECRET);
        assertThat(request.signature()).isEqualTo(expected);
    }

    private void captureAndRespond(HttpExchange exchange, AtomicReference<CapturedRequest> captured) throws IOException {
        captured.set(new CapturedRequest(
                exchange.getRequestURI().getRawPath(),
                exchange.getRequestURI().getRawQuery(),
                exchange.getRequestHeaders().getFirst("Authorization"),
                exchange.getRequestHeaders().getFirst(MangoContextHeaders.TENANT_ID),
                exchange.getRequestHeaders().getFirst(MangoContextHeaders.MEMBER_ID),
                exchange.getRequestHeaders().getFirst("X-Internal-Timestamp"),
                exchange.getRequestHeaders().getFirst("X-Internal-Nonce"),
                exchange.getRequestHeaders().getFirst("X-Internal-Signature")));
        byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    interface ProbeClient {

        @RequestLine("GET /probe?tag=2&tag=1")
        String probe();
    }

    record CapturedRequest(String path, String query, String authorization, String tenantId,
                           String memberId, String timestamp, String nonce, String signature) {
    }
}
