package io.mango.integration.support.client;

import com.sun.net.httpserver.HttpServer;
import io.mango.integration.api.spi.IntegrationDeclarationProvider;
import io.mango.integration.api.vo.IntegrationCredentialFieldVO;
import io.mango.integration.api.vo.IntegrationInstanceDeclarationVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("集成调用客户端测试")
class IntegrationClientTest {

    private HttpServer server;
    private final AtomicInteger tokenRequests = new AtomicInteger();
    private volatile String capturedAuthorization;
    private volatile String capturedBody;
    private int sendStatus = 200;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token", exchange -> {
            tokenRequests.incrementAndGet();
            respond(exchange, "{\"access_token\":\"client-token\",\"expires_in\":3600}");
        });
        server.createContext("/api/send", exchange -> {
            capturedAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
            capturedBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            respond(exchange, sendStatus, sendStatus == 200 ? "{\"errcode\":0}" : "{\"errcode\":500}");
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    @DisplayName("按实例声明读取配置与凭证并发送请求")
    void sendsRequestWithDeclaredAuth() {
        IntegrationClient client = IntegrationClient.of(provider(), "wecom-main", null);

        IntegrationCallResult response = client.post("/api/send", "{\"touser\":\"zhangsan\"}");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(capturedAuthorization).isEqualTo("Bearer client-token");
        assertThat(capturedBody).contains("zhangsan");
        assertThat(tokenRequests.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("目标返回非 2xx 时标记失败并保留状态码")
    void reportsFailureStatus() {
        sendStatus = 500;

        IntegrationCallResult response = IntegrationClient.of(provider(), "wecom-main", null).post("/api/send", "{}");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getStatusCode()).isEqualTo(500);
    }

    @Test
    @DisplayName("实例缺少接口地址时拒绝调用")
    void rejectsWhenBaseUrlMissing() {
        IntegrationDeclarationProvider provider = new FakeProvider("{\"timeoutMs\":\"3000\"}");

        try {
            IntegrationClient.of(provider, "wecom-main", null);
            assertThat(false).isTrue();
        } catch (IllegalStateException exception) {
            assertThat(exception.getMessage()).contains("未配置接口地址");
        }
    }

    private IntegrationDeclarationProvider provider() {
        return new FakeProvider("{\"apiBaseUrl\":\"" + baseUrl() + "\",\"timeoutMs\":\"3000\"}");
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        respond(exchange, 200, body);
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private final class FakeProvider implements IntegrationDeclarationProvider {

        private final String configValuesJson;

        private FakeProvider(String configValuesJson) {
            this.configValuesJson = configValuesJson;
        }

        @Override
        public IntegrationInstanceDeclarationVO instance(String instanceCode) {
            IntegrationInstanceDeclarationVO declaration = new IntegrationInstanceDeclarationVO();
            declaration.setInstanceCode(instanceCode);
            declaration.setConnectorCode("wecom-message");
            declaration.setConfigValuesJson(configValuesJson);
            declaration.setCredentialCode("wecom-main-credential");
            return declaration;
        }

        @Override
        public String connectorAuthSchema(String connectorCode) {
            return "{\"authMode\":\"OAUTH2\",\"params\":{\"tokenUrl\":\"" + baseUrl() + "/token\","
                    + "\"clientIdParam\":\"corpid\",\"clientSecretParam\":\"corpsecret\","
                    + "\"clientIdField\":\"corpId\",\"clientSecretField\":\"corpSecret\","
                    + "\"tokenField\":\"access_token\",\"expiresField\":\"expires_in\"}}";
        }

        @Override
        public List<IntegrationCredentialFieldVO> credentials(String credentialCode) {
            IntegrationCredentialFieldVO corpId = new IntegrationCredentialFieldVO();
            corpId.setFieldCode("corpId");
            corpId.setFieldValue("wx-corp");
            IntegrationCredentialFieldVO secret = new IntegrationCredentialFieldVO();
            secret.setFieldCode("corpSecret");
            secret.setFieldValue("corp-secret");
            return List.of(corpId, secret);
        }
    }
}
