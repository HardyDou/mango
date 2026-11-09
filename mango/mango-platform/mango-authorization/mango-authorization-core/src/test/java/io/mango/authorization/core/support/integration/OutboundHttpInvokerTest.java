package io.mango.authorization.core.support.integration;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OutboundHttpInvoker 单元测试")
class OutboundHttpInvokerTest {

    private HttpServer server;
    private String capturedPath;
    private int responseStatus = 200;
    private int requestCount;
    private int failFirstRequestCount;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestCount++;
            if (failFirstRequestCount > 0) {
                failFirstRequestCount--;
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
                return;
            }
            capturedPath = exchange.getRequestURI().getPath();
            exchange.sendResponseHeaders(responseStatus, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    @DisplayName("可访问目标返回成功和状态码")
    void returnsSuccessForReachableTarget() {
        OutboundResult result = new OutboundHttpInvoker(true).invoke(call(baseUrl(), 0));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatusCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("连接成功");
    }

    @Test
    @DisplayName("非 2xx/3xx 返回不可达")
    void returnsFailureForErrorStatus() {
        responseStatus = 500;

        OutboundResult result = new OutboundHttpInvoker(true).invoke(call(baseUrl(), 0));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatusCode()).isEqualTo(500);
    }

    @Test
    @DisplayName("失败后按配置重试并在下一次成功")
    void retriesWhenConfigured() {
        failFirstRequestCount = 1;

        OutboundResult result = new OutboundHttpInvoker(true).invoke(call(baseUrl(), 1));

        assertThat(result.isSuccess()).isTrue();
        assertThat(requestCount).isEqualTo(2);
    }

    @Test
    @DisplayName("未配置重试时失败只请求一次")
    void doesNotRetryWhenNotConfigured() {
        failFirstRequestCount = 1;

        OutboundResult result = new OutboundHttpInvoker(true).invoke(call(baseUrl(), 0));

        assertThat(result.isSuccess()).isFalse();
        assertThat(requestCount).isEqualTo(1);
    }

    @Test
    @DisplayName("基础地址带路径且测试路径为斜杠时直接使用基础路径")
    void basePathWithoutSubPathIsUsedDirectly() {
        OutboundCall request = OutboundCall.builder()
                .baseUrl(baseUrl() + "/actuator/health")
                .path("/")
                .timeoutMs(3000)
                .build();

        OutboundResult result = new OutboundHttpInvoker(true).invoke(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(capturedPath).isEqualTo("/actuator/health");
    }

    @Test
    @DisplayName("默认配置拒绝回环目标且不发起请求")
    void privateTargetRejectedByDefault() {
        OutboundResult result = new OutboundHttpInvoker(false).invoke(call(baseUrl(), 0));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("目标地址不在允许范围内");
        assertThat(result.getStatusCode()).isNull();
        assertThat(requestCount).isZero();
    }

    @Test
    @DisplayName("非 http 地址被拒绝")
    void invalidBaseUrlRejected() {
        OutboundResult result = new OutboundHttpInvoker(true)
                .invoke(OutboundCall.builder().baseUrl("ftp://example.com").path("/").timeoutMs(3000).build());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("目标地址必须是http或https");
    }

    private OutboundCall call(String baseUrl, int retryMax) {
        return OutboundCall.builder()
                .baseUrl(baseUrl)
                .path("/")
                .timeoutMs(3000)
                .retryMax(retryMax)
                .build();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }
}
