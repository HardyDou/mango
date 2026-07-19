package io.mango.payment.starter.remote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.mango.payment.api.PaymentApplicationApi;
import io.mango.payment.api.PaymentChannelCallbackApi;
import io.mango.payment.api.PaymentOpenApi;
import io.mango.payment.api.PaymentSecurityApi;
import io.mango.payment.api.command.PaymentChannelCallbackCommand;
import io.mango.payment.api.command.PaymentOpenRequestCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRemoteHttpIntegrationTest {

    private static final List<RecordedRequest> REQUESTS = new CopyOnWriteArrayList<>();

    private static HttpServer server;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", PaymentRemoteHttpIntegrationTest::handle);
        server.start();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @BeforeEach
    void clearRequests() {
        REQUESTS.clear();
    }

    @Test
    void injectedApiBeansSendRepresentativePaymentContractsOverRealHttp() {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class,
                        HttpMessageConvertersAutoConfiguration.class,
                        FeignAutoConfiguration.class,
                        io.mango.infra.feign.starter.FeignAutoConfiguration.class,
                        PaymentRemoteAutoConfiguration.class))
                .withPropertyValues(
                        "spring.cloud.openfeign.client.config.paymentApplicationFeignClient.url=" + baseUrl,
                        "spring.cloud.openfeign.client.config.paymentSecurityFeignClient.url=" + baseUrl,
                        "spring.cloud.openfeign.client.config.paymentChannelCallbackFeignClient.url=" + baseUrl,
                        "spring.cloud.openfeign.client.config.paymentOpenFeignClient.url=" + baseUrl,
                        "mango.feign.module-target-enabled=false",
                        "mango.internal-call.secret=payment-issue-570-secret");

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            PaymentConfigPageQuery pageQuery = new PaymentConfigPageQuery();
            pageQuery.setKeyword("IT_570");
            pageQuery.setPage(3L);
            pageQuery.setSize(25L);
            context.getBean(PaymentApplicationApi.class).pageApplications(pageQuery);

            context.getBean(PaymentSecurityApi.class).reencryptSensitiveFields(7);

            PaymentChannelCallbackCommand callback = new PaymentChannelCallbackCommand();
            callback.setCallbackType("PAYMENT");
            callback.setChannelCode("IT_570_CHANNEL");
            callback.setChannelMerchantNo("IT_570_MERCHANT");
            callback.setChannelStatus("SUCCESS");
            callback.setAmount(570L);
            context.getBean(PaymentChannelCallbackApi.class).handle(callback);

            PaymentOpenRequestCommand openRequest = new PaymentOpenRequestCommand();
            openRequest.setAppId("IT_570_APP");
            openRequest.setTenantId("IT_570_TENANT");
            openRequest.setTimestamp("570");
            openRequest.setNonce("IT_570_NONCE");
            openRequest.setSignature("IT_570_SIGNATURE");
            context.getBean(PaymentOpenApi.class).createOrder(openRequest);
        });

        assertThat(REQUESTS).extracting(RecordedRequest::methodAndPath).containsExactly(
                "GET /payment/applications/page",
                "POST /payment/security/sensitive-fields/reencrypt",
                "POST /payment/channel-callbacks",
                "POST /openapi/pay/orders/create");
        assertThat(REQUESTS.get(0).uri().getQuery()).contains("keyword=IT_570", "page=3", "size=25");
        assertThat(REQUESTS.get(1).uri().getQuery()).isEqualTo("limit=7");
        assertThat(REQUESTS.get(2).body()).contains(
                "\"channelCode\":\"IT_570_CHANNEL\"", "\"amount\":570");
        assertThat(REQUESTS.get(3).body()).contains(
                "\"appId\":\"IT_570_APP\"", "\"signature\":\"IT_570_SIGNATURE\"");
        assertThat(REQUESTS).allSatisfy(request -> {
            assertThat(request.internalCall()).isEqualTo("true");
            assertThat(request.signature()).isNotBlank();
        });
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        REQUESTS.add(new RecordedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI(),
                body,
                exchange.getRequestHeaders().getFirst("X-Internal-Call"),
                exchange.getRequestHeaders().getFirst("X-Internal-Signature")));
        byte[] response = "{\"code\":200,\"success\":true,\"msg\":\"success\",\"data\":null}"
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private record RecordedRequest(
            String method, URI uri, String body, String internalCall, String signature) {

        String methodAndPath() {
            return method + " " + uri.getPath();
        }
    }
}
