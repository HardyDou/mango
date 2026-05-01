package io.mango.authorization.security.core.impl;

import io.mango.authorization.api.security.ITokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("JWT token performance baseline tests")
class JjwtTokenServicePerformanceBaselineTest {

    private ITokenProvider tokenService;

    @BeforeEach
    void setUp() {
        JjwtTokenServiceImpl impl = new JjwtTokenServiceImpl(null);
        setField(impl, "newSecret", "mango-secret-key-for-jwt-token-generation-must-be-at-least-256-bits");
        setField(impl, "legacySecret", "");
        setField(impl, "accessTokenValiditySeconds", 7200L);
        setField(impl, "refreshTokenValiditySeconds", 604800L);
        impl.init();
        this.tokenService = impl;
    }

    @Test
    @DisplayName("token validate and claim extraction should stay within baseline")
    void tokenValidateAndClaimExtractionShouldStayWithinBaseline() {
        String token = tokenService.generateAccessToken(1L, "perf-user", Map.of("scope", "baseline"));

        assertTimeout(Duration.ofSeconds(4), () -> {
            long startedAt = System.nanoTime();
            for (int i = 0; i < 5_000; i++) {
                assertTrue(tokenService.validateToken(token));
                assertTrue(tokenService.getUserId(token) > 0);
                assertTrue(tokenService.getUsername(token).startsWith("perf"));
                assertTrue(tokenService.getTokenType(token).equals(ITokenProvider.TOKEN_TYPE_ACCESS));
            }
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
            assertTrue(elapsedMillis < 4_000, "JWT baseline exceeded: " + elapsedMillis + "ms");
        });
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
