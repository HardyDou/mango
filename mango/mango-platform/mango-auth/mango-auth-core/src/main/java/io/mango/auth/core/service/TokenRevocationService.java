package io.mango.auth.core.service;

import io.mango.infra.kv.api.IKvStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 已撤销令牌追踪服务。
 */
@Slf4j
@Service
public class TokenRevocationService {

    private static final String KEY_PREFIX = "auth:token:revoked:";

    private final ObjectProvider<IKvStore> kvStoreProvider;

    public TokenRevocationService(ObjectProvider<IKvStore> kvStoreProvider) {
        this.kvStoreProvider = kvStoreProvider;
    }

    public void revoke(String token, long ttlSeconds) {
        String normalized = normalize(token);
        if (normalized == null || ttlSeconds <= 0) {
            return;
        }
        IKvStore kvStore = kvStoreProvider.getIfAvailable();
        if (kvStore == null) {
            log.warn("Token revocation skipped because IKvStore is not available");
            return;
        }
        kvStore.set(key(normalized), "1", ttlSeconds);
    }

    public boolean isRevoked(String token) {
        String normalized = normalize(token);
        if (normalized == null) {
            return false;
        }
        IKvStore kvStore = kvStoreProvider.getIfAvailable();
        return kvStore != null && kvStore.exists(key(normalized));
    }

    private String key(String token) {
        return KEY_PREFIX + sha256(token);
    }

    private String normalize(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String trimmed = token.trim();
        if (trimmed.startsWith("Bearer ")) {
            return trimmed.substring("Bearer ".length());
        }
        return trimmed;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
