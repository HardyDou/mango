package io.mango.authorization.security.core.impl;

import io.mango.authorization.api.security.ITokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JjwtTokenServiceImpl}.
 */
class JjwtTokenServiceImplTest {

    private ITokenProvider tokenService;

    @BeforeEach
    void setUp() {
        // Pass null for IKvStore (no blacklist in unit tests)
        JjwtTokenServiceImpl impl = new JjwtTokenServiceImpl(null);
        // Inject test values via reflection (simulates @Value injection)
        setField(impl, "newSecret", "mango-secret-key-for-jwt-token-generation-must-be-at-least-256-bits");
        setField(impl, "legacySecret", "");
        setField(impl, "accessTokenValiditySeconds", 7200L);
        setField(impl, "refreshTokenValiditySeconds", 604800L);
        impl.init();
        this.tokenService = impl;
    }

    @Test
    void generateAccessToken_validInput_returnsToken() {
        String token = tokenService.generateAccessToken(1L, "admin", Map.of("role", "admin"));
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void generateRefreshToken_validInput_returnsToken() {
        String token = tokenService.generateRefreshToken(1L, "admin");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void validateToken_validAccessToken_returnsTrue() {
        String token = tokenService.generateAccessToken(1L, "admin", Map.of());
        assertTrue(tokenService.validateToken(token));
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        assertFalse(tokenService.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_nullToken_returnsFalse() {
        assertFalse(tokenService.validateToken(null));
    }

    @Test
    void init_withoutSecret_throwsException() {
        JjwtTokenServiceImpl impl = new JjwtTokenServiceImpl(null);
        setField(impl, "newSecret", "");
        setField(impl, "legacySecret", "");

        assertThrows(IllegalStateException.class, impl::init);
    }

    @Test
    void init_withShortSecret_throwsException() {
        JjwtTokenServiceImpl impl = new JjwtTokenServiceImpl(null);
        setField(impl, "newSecret", "short-secret");
        setField(impl, "legacySecret", "");

        assertThrows(IllegalStateException.class, impl::init);
    }

    @Test
    void getUserId_validToken_returnsUserId() {
        String token = tokenService.generateAccessToken(42L, "testuser", Map.of());
        assertEquals(42L, tokenService.getUserId(token));
    }

    @Test
    void getUsername_validToken_returnsUsername() {
        String token = tokenService.generateAccessToken(1L, "admin", Map.of());
        assertEquals("admin", tokenService.getUsername(token));
    }

    @Test
    void getTokenType_accessToken_returnsAccess() {
        String token = tokenService.generateAccessToken(1L, "admin", Map.of());
        assertEquals("access", tokenService.getTokenType(token));
    }

    @Test
    void getTokenType_refreshToken_returnsRefresh() {
        String token = tokenService.generateRefreshToken(1L, "admin");
        assertEquals("refresh", tokenService.getTokenType(token));
    }

    @Test
    void refresh_validRefreshToken_returnsNewTokenPair() {
        String refreshToken = tokenService.generateRefreshToken(99L, "refreshuser");
        ITokenProvider.TokenPair pair = tokenService.refresh(refreshToken);

        assertNotNull(pair);
        assertNotNull(pair.accessToken());
        assertNotNull(pair.refreshToken());
        assertEquals("access", tokenService.getTokenType(pair.accessToken()));
        assertEquals("refresh", tokenService.getTokenType(pair.refreshToken()));
    }

    @Test
    void refresh_invalidToken_returnsNull() {
        assertNull(tokenService.refresh("invalid.token"));
    }

    @Test
    void refresh_accessTokenNotAccepted_returnsNull() {
        String accessToken = tokenService.generateAccessToken(1L, "admin", Map.of());
        assertNull(tokenService.refresh(accessToken));
    }

    @Test
    void getUserId_invalidToken_returnsNull() {
        assertNull(tokenService.getUserId("invalid.token.here"));
    }

    @Test
    void getUsername_invalidToken_returnsNull() {
        assertNull(tokenService.getUsername("invalid.token.here"));
    }

    @Test
    void getTokenType_invalidToken_returnsNull() {
        assertNull(tokenService.getTokenType("invalid.token"));
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
