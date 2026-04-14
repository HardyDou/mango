package io.mango.infra.security.core.impl;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.security.api.ITokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JjwtTokenServiceImpl refresh token replay protection (jti blacklist).
 */
class JjwtTokenServiceImplBlacklistTest {

    private ITokenService tokenService;
    private IKvStore kvStore;

    @BeforeEach
    void setUp() {
        kvStore = mock(IKvStore.class);
        JjwtTokenServiceImpl impl = new JjwtTokenServiceImpl(kvStore);
        setField(impl, "newSecret", "mango-secret-key-for-jwt-token-generation-must-be-at-least-256-bits");
        setField(impl, "legacySecret", "");
        setField(impl, "accessTokenValiditySeconds", 7200L);
        setField(impl, "refreshTokenValiditySeconds", 604800L);
        impl.init();
        this.tokenService = impl;
    }

    @Test
    void refresh_withReplayToken_rejectedByBlacklist() {
        // First refresh: token not in blacklist, put() returns true
        when(kvStore.put(anyString(), eq("1"), anyLong())).thenReturn(true);

        String refreshToken = tokenService.generateRefreshToken(1L, "admin");
        ITokenService.TokenPair firstRefresh = tokenService.refresh(refreshToken);
        assertNotNull(firstRefresh, "First refresh should succeed");

        // Simulate blacklist check: jti key already exists (replay attack)
        // put() returns false means key was already in blacklist
        reset(kvStore);
        when(kvStore.put(anyString(), eq("1"), anyLong())).thenReturn(false);

        // Second refresh with SAME token: should be rejected as replay
        ITokenService.TokenPair secondRefresh = tokenService.refresh(refreshToken);
        assertNull(secondRefresh, "Replay refresh token should be rejected");
    }

    @Test
    void refresh_withoutKvStore_stillWorks() {
        // When IKvStore is null, replay protection is skipped (graceful degradation)
        JjwtTokenServiceImpl implNoKv = new JjwtTokenServiceImpl(null);
        setField(implNoKv, "newSecret", "mango-secret-key-for-jwt-token-generation-must-be-at-least-256-bits");
        setField(implNoKv, "legacySecret", "");
        setField(implNoKv, "accessTokenValiditySeconds", 7200L);
        setField(implNoKv, "refreshTokenValiditySeconds", 604800L);
        implNoKv.init();

        String refreshToken = implNoKv.generateRefreshToken(1L, "admin");
        // Should succeed even without KV store (no blacklist check)
        ITokenService.TokenPair pair = implNoKv.refresh(refreshToken);
        assertNotNull(pair, "Refresh should succeed without IKvStore");
    }

    @Test
    void refresh_blacklistUsesCorrectPrefix() {
        // Verify the blacklist key format is "jwt:refresh:jti:{jti}"
        when(kvStore.put(anyString(), eq("1"), anyLong())).thenReturn(true);

        String refreshToken = tokenService.generateRefreshToken(1L, "admin");
        tokenService.refresh(refreshToken);

        // Capture the key argument passed to kvStore.put()
        verify(kvStore).put(argThat((String key) -> key.startsWith("jwt:refresh:jti:")),
                eq("1"),
                longThat(val -> val > 0));
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
