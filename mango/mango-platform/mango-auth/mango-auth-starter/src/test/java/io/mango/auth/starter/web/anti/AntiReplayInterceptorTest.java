package io.mango.auth.starter.web.anti;

import io.mango.auth.core.anti.IdempotencyGuard;
import io.mango.auth.core.anti.ReplayGuard;
import io.mango.auth.core.anti.SignatureValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DisplayName("防重放签名拦截器测试")
class AntiReplayInterceptorTest {

    @Test
    @DisplayName("配置了 appKey 密钥时应校验签名并放行")
    void preHandle_shouldPassWhenConfiguredSecretMatchesSignature() throws Exception {
        String appKey = "demo-app";
        String secret = "demo-secret";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String body = "{\"name\":\"mango\"}";
        SignatureValidator validator = new SignatureValidator();
        String data = validator.buildSignatureData(appKey, secret, timestamp, body);
        String sign = validator.computeSignature("MD5", data);

        MockHttpServletRequest request = signedRequest(appKey, timestamp, body, sign);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean passed = newInterceptor(Map.of(appKey, secret), null, false)
                .preHandle(request, response, new Object());

        assertTrue(passed);
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("未知 appKey 且未开启兜底密钥时应拒绝签名请求")
    void preHandle_shouldRejectWhenAppSecretMissing() throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis());
        MockHttpServletRequest request = signedRequest("missing-app", timestamp, "", "bad-sign");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean passed = newInterceptor(Map.of(), null, false)
                .preHandle(request, response, new Object());

        assertFalse(passed);
        assertEquals(401, response.getStatus());
        assertEquals("{\"code\":401,\"msg\":\"Invalid signature\"}", response.getContentAsString());
    }

    @Test
    @DisplayName("显式开启兜底密钥时未知 appKey 可使用默认密钥校验")
    void preHandle_shouldUseFallbackSecretOnlyWhenEnabled() throws Exception {
        String appKey = "unknown-app";
        String secret = "fallback-secret";
        String timestamp = String.valueOf(System.currentTimeMillis());
        SignatureValidator validator = new SignatureValidator();
        String data = validator.buildSignatureData(appKey, secret, timestamp, "");
        String sign = validator.computeSignature("MD5", data);
        MockHttpServletRequest request = signedRequest(appKey, timestamp, "", sign);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean passed = newInterceptor(Map.of(), secret, true)
                .preHandle(request, response, new Object());

        assertTrue(passed);
        assertEquals(200, response.getStatus());
    }

    private AntiReplayInterceptor newInterceptor(Map<String, String> appSecrets,
                                                 String defaultSecret,
                                                 boolean allowFallback) {
        AntiReplayProperties properties = new AntiReplayProperties();
        properties.setAppSecrets(appSecrets);
        properties.setDefaultSecret(defaultSecret);
        properties.setAllowFallback(allowFallback);
        return new AntiReplayInterceptor(
                mock(ReplayGuard.class),
                mock(IdempotencyGuard.class),
                new SignatureValidator(),
                new ConfiguredAppSecretProvider(properties));
    }

    private MockHttpServletRequest signedRequest(String appKey, String timestamp, String body, String sign) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.addHeader("X-Sign-Algorithm", "MD5");
        request.addHeader("X-App-Key", appKey);
        request.addHeader("X-Request-Timestamp", timestamp);
        request.addHeader("X-Sign", sign);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
