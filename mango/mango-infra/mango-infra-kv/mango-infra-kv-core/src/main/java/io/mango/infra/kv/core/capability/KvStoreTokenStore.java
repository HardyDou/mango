package io.mango.infra.kv.core.capability;

import io.mango.common.result.Require;
import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.api.ITokenStore;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KvStoreTokenStore implements ITokenStore {

    private final IKvStore kvStore;

    @Override
    public void store(String token, String value, long ttlSeconds) {
        validateToken(token);
        validateValue(value);
        validateTtl(ttlSeconds);
        kvStore.set(token, value, ttlSeconds);
    }

    @Override
    public String get(String token) {
        validateToken(token);
        return kvStore.get(token);
    }

    @Override
    public void remove(String token) {
        validateToken(token);
        kvStore.delete(token);
    }

    private void validateToken(String token) {
        Require.notBlank(token, "token cannot be null or blank");
    }

    private void validateValue(String value) {
        Require.notNull(value, "value cannot be null");
    }

    private void validateTtl(long ttlSeconds) {
        Require.positive(ttlSeconds, "ttlSeconds must be positive");
    }
}
