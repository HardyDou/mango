package io.mango.captcha.starter.remote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaRemoteHttpIntegrationTest {

    private static final List<RecordedRequest> REQUESTS = new CopyOnWriteArrayList<>();

    private static HttpServer server;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", CaptchaRemoteHttpIntegrationTest::handle);
        server.start();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void remoteBeanInvokesEveryCaptchaEndpointOverRealHttp() {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class,
                        HttpMessageConvertersAutoConfiguration.class,
                        org.springframework.cloud.openfeign.FeignAutoConfiguration.class,
                        io.mango.infra.feign.starter.FeignAutoConfiguration.class,
                        CaptchaRemoteAutoConfiguration.class))
                .withPropertyValues(
                        "spring.cloud.openfeign.client.config.mango-captcha.url=" + baseUrl,
                        "spring.cloud.openfeign.client.config.captchaFeignClient.url=" + baseUrl,
                        "mango.feign.module-target-enabled=false",
                        "mango.internal-call.secret=captcha-integration-secret");

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            CaptchaFeignClient client = context.getBean(CaptchaFeignClient.class);

            assertThat(client.getTypes().getData().getCurrentStorage()).isEqualTo("TestKvStore");
            assertThat(client.generateArithmetic().getData().getKey()).isEqualTo("remote-key");
            assertThat(client.generateBlockPuzzle().getData().getKey()).isEqualTo("remote-key");
            assertThat(client.generateClickWord().getData().getKey()).isEqualTo("remote-key");
            assertThat(client.generateBehavior().getData().getKey()).isEqualTo("remote-key");

            CaptchaVerifyRequest verifyRequest = new CaptchaVerifyRequest();
            verifyRequest.setKey("remote-key");
            verifyRequest.setType(CaptchaType.ARITHMETIC);
            verifyRequest.setCode("7");
            assertThat(client.verifyBehavior(verifyRequest).getData().isPassed()).isTrue();
            assertThat(client.verify(verifyRequest).getData()).isTrue();

            CaptchaSendRequest sendRequest = new CaptchaSendRequest();
            sendRequest.setType(CaptchaType.SMS);
            sendRequest.setTarget("13800138000");
            sendRequest.setBusinessType("login");
            assertThat(client.send(sendRequest).getData()).isEqualTo("captcha:login:13800138000");
        });

        assertThat(REQUESTS).extracting(RecordedRequest::methodAndPath).containsExactly(
                "GET /captcha/types",
                "GET /captcha/arithmetic",
                "GET /captcha/block-puzzle",
                "GET /captcha/click-word",
                "GET /captcha/behavior",
                "POST /captcha/behavior/verify",
                "POST /captcha/verify",
                "POST /captcha/send");
        assertThat(REQUESTS).allSatisfy(request -> {
            assertThat(request.internalCall()).isEqualTo("true");
            assertThat(request.signature()).isNotBlank();
        });
        assertThat(REQUESTS.get(5).body()).contains("\"key\":\"remote-key\"", "\"code\":\"7\"");
        assertThat(REQUESTS.get(7).body()).contains("\"target\":\"13800138000\"", "\"businessType\":\"login\"");
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        REQUESTS.add(new RecordedRequest(
                exchange.getRequestMethod(),
                path,
                body,
                exchange.getRequestHeaders().getFirst("X-Internal-Call"),
                exchange.getRequestHeaders().getFirst("X-Internal-Signature")));
        byte[] response = responseJson(path).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private static String responseJson(String path) {
        if ("/captcha/types".equals(path)) {
            return "{\"code\":200,\"success\":true,\"msg\":\"success\","
                    + "\"data\":{\"types\":[\"ARITHMETIC\"],\"currentStorage\":\"TestKvStore\"}}";
        }
        if ("/captcha/behavior/verify".equals(path)) {
            return "{\"code\":200,\"success\":true,\"msg\":\"success\","
                    + "\"data\":{\"key\":\"remote-key\",\"score\":0.9,\"passed\":true,"
                    + "\"riskLevel\":\"LOW\",\"suggestAction\":\"ALLOW\",\"reason\":\"OK\"}}";
        }
        if ("/captcha/verify".equals(path)) {
            return "{\"code\":200,\"success\":true,\"msg\":\"success\",\"data\":true}";
        }
        if ("/captcha/send".equals(path)) {
            return "{\"code\":200,\"success\":true,\"msg\":\"success\","
                    + "\"data\":\"captcha:login:13800138000\"}";
        }
        return "{\"code\":200,\"success\":true,\"msg\":\"success\","
                + "\"data\":{\"key\":\"remote-key\",\"type\":\"ARITHMETIC\",\"expireTime\":300}}";
    }

    private record RecordedRequest(String method, String path, String body, String internalCall, String signature) {

        String methodAndPath() {
            return method + " " + path;
        }
    }
}
