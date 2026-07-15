package io.mango.auth.starter.flow;

import io.mango.auth.api.command.ChangeRequiredPasswordCommand;
import io.mango.auth.api.command.LoginCommand;
import io.mango.auth.api.command.LoginTenantOptionsCommand;
import io.mango.auth.api.command.SendAuthCaptchaCommand;
import io.mango.auth.api.command.WecomLoginCommand;
import io.mango.auth.api.vo.LoginTenantVO;
import io.mango.auth.api.vo.LoginVO;
import io.mango.auth.api.vo.WecomLoginConfigVO;
import io.mango.auth.core.anti.AppSecretProvider;
import io.mango.auth.core.anti.IdempotencyGuard;
import io.mango.auth.core.anti.ReplayGuard;
import io.mango.auth.core.anti.SignatureValidator;
import io.mango.auth.core.service.IAuthService;
import io.mango.auth.starter.controller.AuthController;
import io.mango.auth.starter.web.AuthCookieResponseAdvice;
import io.mango.auth.starter.web.LogoutHeaderCompatibilityFilter;
import io.mango.auth.starter.web.anti.AntiReplayFilter;
import io.mango.common.result.R;
import io.mango.infra.kv.api.IKvStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("flow")
@Tag("auth")
@DisplayName("认证真实 HTTP 入口流程测试")
@SpringBootTest(
        classes = AuthHttpEntryFlowTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthHttpEntryFlowTest {

    @LocalServerPort
    private int port;

    @jakarta.annotation.Resource
    private TestAuthService authService;

    @jakarta.annotation.Resource
    private EchoController echoController;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    @DisplayName("登录应通过真实 Tomcat 返回令牌 Cookie")
    void loginShouldWriteAuthenticationCookie() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/auth/login"))
                .header("Content-Type", "application/json")
                .header("User-Agent", "mango-auth-flow")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"username":"admin","password":"admin123","tenantId":"1"}
                        """))
                .build());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"success\":true", "\"accessToken\":\"access-token\"");
        assertThat(response.headers().allValues("Set-Cookie"))
                .anyMatch(value -> value.contains("MANGO_TOKEN=access-token")
                        && value.contains("HttpOnly") && value.contains("SameSite=Lax"));
        assertThat(authService.loginCommand.get().getUserAgent()).isEqualTo("mango-auth-flow");
    }

    @Test
    @DisplayName("API 契约参数校验应在真实 Controller 入口生效")
    void invalidLoginShouldBeRejectedBeforeService() throws Exception {
        authService.loginCommand.set(null);

        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"username":"","password":"","tenantId":"1"}
                        """))
                .build());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(authService.loginCommand.get()).isNull();
    }

    @Test
    @DisplayName("仅 Authorization 请求头的历史退出协议仍应可用并清理 Cookie")
    void headerOnlyLogoutShouldRemainCompatible() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/auth/logout"))
                .header("Authorization", "Bearer access-token")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"success\":true");
        assertThat(authService.logoutToken.get()).isEqualTo("Bearer access-token");
        assertThat(response.headers().allValues("Set-Cookie"))
                .anyMatch(value -> value.contains("MANGO_TOKEN=") && value.contains("Max-Age=0"));
    }

    @Test
    @DisplayName("签名过滤器校验后真实 Controller 仍应收到完整 JSON")
    void signedJsonShouldReachControllerThroughRealServletChain() throws Exception {
        String body = "{\"name\":\"mango\"}";
        String timestamp = String.valueOf(System.currentTimeMillis());
        SignatureValidator validator = new SignatureValidator();
        String signature = validator.computeSignature("MD5",
                validator.buildSignatureData("flow-app", "flow-secret", timestamp, body));

        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/flow/echo"))
                .header("Content-Type", "application/json")
                .header("X-Sign-Algorithm", "MD5")
                .header("X-App-Key", "flow-app")
                .header("X-Request-Timestamp", timestamp)
                .header("X-Sign", signature)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"data\":\"mango\"");
    }

    @Test
    @DisplayName("幂等键重复请求应复用首次成功响应")
    void idempotentSuccessShouldReturnCachedResponse() throws Exception {
        echoController.successCalls.set(0);
        String key = "FLOW_SUCCESS_" + System.nanoTime();
        HttpRequest request = jsonPost("/flow/idempotent", key);

        HttpResponse<String> first = send(request);
        HttpResponse<String> second = send(jsonPost("/flow/idempotent", key));

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(first.body()).contains("\"data\":\"mango:1\"");
        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(second.body()).isEqualTo(first.body());
        assertThat(echoController.successCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("幂等请求失败后应释放占位并允许重试")
    void idempotentFailureShouldRemainRetryable() throws Exception {
        echoController.retryCalls.set(0);
        String key = "FLOW_RETRY_" + System.nanoTime();

        HttpResponse<String> first = send(jsonPost("/flow/idempotent-retry", key));
        HttpResponse<String> second = send(jsonPost("/flow/idempotent-retry", key));

        assertThat(first.statusCode()).isEqualTo(500);
        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(second.body()).contains("\"data\":\"recovered:2\"");
        assertThat(echoController.retryCalls.get()).isEqualTo(2);
    }

    private HttpRequest jsonPost(String path, String idempotencyKey) {
        return HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header("X-Idempotency-Key", idempotencyKey)
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"mango\"}"))
                .build();
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
            ServletWebServerFactoryAutoConfiguration.class,
            DispatcherServletAutoConfiguration.class,
            WebMvcAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class,
            ValidationAutoConfiguration.class
    })
    @Import({
            AuthController.class,
            AuthCookieResponseAdvice.class,
            LogoutHeaderCompatibilityFilter.class,
            AntiReplayFilter.class,
            EchoController.class
    })
    static class TestApp {

        @Bean
        TestAuthService authService() {
            return new TestAuthService();
        }

        @Bean
        IKvStore kvStore() {
            return new InMemoryKvStore();
        }

        @Bean
        ReplayGuard replayGuard(IKvStore kvStore) {
            return new ReplayGuard(kvStore);
        }

        @Bean
        IdempotencyGuard idempotencyGuard(IKvStore kvStore) {
            return new IdempotencyGuard(kvStore);
        }

        @Bean
        SignatureValidator signatureValidator() {
            return new SignatureValidator();
        }

        @Bean
        AppSecretProvider appSecretProvider() {
            return appKey -> "flow-app".equals(appKey) ? "flow-secret" : null;
        }
    }

    @RestController
    static class EchoController {
        private final AtomicInteger successCalls = new AtomicInteger();
        private final AtomicInteger retryCalls = new AtomicInteger();

        @PostMapping("/flow/echo")
        R<String> echo(@RequestBody EchoBody body) {
            return R.ok(body.name());
        }

        @PostMapping("/flow/idempotent")
        R<String> idempotent(@RequestBody EchoBody body) {
            return R.ok(body.name() + ":" + successCalls.incrementAndGet());
        }

        @PostMapping("/flow/idempotent-retry")
        ResponseEntity<R<String>> idempotentRetry(@RequestBody EchoBody body) {
            int invocation = retryCalls.incrementAndGet();
            if (invocation == 1) {
                return ResponseEntity.internalServerError().body(R.fail(500, "retry"));
            }
            return ResponseEntity.ok(R.ok("recovered:" + invocation));
        }
    }

    record EchoBody(String name) {
    }

    static final class TestAuthService implements IAuthService {
        private final AtomicReference<LoginCommand> loginCommand = new AtomicReference<>();
        private final AtomicReference<String> logoutToken = new AtomicReference<>();

        @Override
        public LoginVO login(LoginCommand command) {
            loginCommand.set(command);
            LoginVO response = new LoginVO();
            response.setAccessToken("access-token");
            response.setRefreshToken("refresh-token");
            response.setUserId(1L);
            response.setUsername(command.getUsername());
            return response;
        }

        @Override
        public LoginVO changeRequiredPassword(ChangeRequiredPasswordCommand command) {
            return login(new LoginCommand());
        }

        @Override
        public LoginVO loginByWecom(WecomLoginCommand command) {
            return login(new LoginCommand());
        }

        @Override
        public WecomLoginConfigVO getWecomLoginConfig(String tenantId) {
            return new WecomLoginConfigVO();
        }

        @Override
        public List<LoginTenantVO> listLoginTenants(LoginTenantOptionsCommand command) {
            return List.of();
        }

        @Override
        public void logout(String token) {
            logoutToken.set(token);
        }

        @Override
        public LoginVO refreshToken(String refreshToken) {
            return new LoginVO();
        }

        @Override
        public boolean validateToken(String token) {
            return true;
        }

        @Override
        public LoginVO info(String authorization) {
            return new LoginVO();
        }

        @Override
        public String sendCaptcha(SendAuthCaptchaCommand command) {
            return "captcha-key";
        }
    }

    static final class InMemoryKvStore implements IKvStore {
        private final Map<String, String> values = new ConcurrentHashMap<>();

        @Override
        public boolean setIfAbsent(String key, String value, long expireSeconds) {
            return values.putIfAbsent(key, value) == null;
        }

        @Override
        public String get(String key) {
            return values.get(key);
        }

        @Override
        public void delete(String key) {
            values.remove(key);
        }

        @Override
        public boolean exists(String key) {
            return values.containsKey(key);
        }
    }
}
