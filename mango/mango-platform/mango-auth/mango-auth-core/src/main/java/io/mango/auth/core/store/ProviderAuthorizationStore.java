package io.mango.auth.core.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.auth.api.enums.AuthCode;
import io.mango.auth.api.enums.ExternalAuthProvider;
import io.mango.auth.api.enums.ProviderAuthorizationIntent;
import io.mango.common.result.Require;
import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class ProviderAuthorizationStore {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String STATE_PREFIX = "auth:provider:state:";
    private static final String BIND_PREFIX = "auth:provider:binding:";

    private final IKvStore kvStore;
    private final ObjectMapper objectMapper;

    @Value("${mango.auth.provider-state-ttl-seconds:600}")
    private long stateTtlSeconds;

    @Value("${mango.auth.provider-bind-ticket-ttl-seconds:600}")
    private long bindTicketTtlSeconds;

    public String issueState(StatePayload payload) {
        return issue(STATE_PREFIX, payload, stateTtlSeconds);
    }

    public StatePayload consumeState(String token) {
        return consume(STATE_PREFIX, token, StatePayload.class, AuthCode.PROVIDER_STATE_INVALID);
    }

    public String issueBinding(BindingPayload payload) {
        return issue(BIND_PREFIX, payload, bindTicketTtlSeconds);
    }

    public BindingPayload consumeBinding(String token) {
        return consume(BIND_PREFIX, token, BindingPayload.class, AuthCode.EXTERNAL_BIND_TICKET_INVALID);
    }

    public long stateTtlSeconds() {
        return stateTtlSeconds;
    }

    public long bindTicketTtlSeconds() {
        return bindTicketTtlSeconds;
    }

    private String issue(String prefix, Object payload, long ttl) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String value;
        try {
            value = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            return Require.fail(AuthCode.AUTH_REQUEST_INVALID, "第三方授权状态无法保存", exception);
        }
        Require.isTrue(kvStore.setIfAbsent(prefix + token, value, ttl), AuthCode.AUTH_REQUEST_INVALID);
        return token;
    }

    private <T> T consume(String prefix, String token, Class<T> type, AuthCode errorCode) {
        Require.isTrue(token != null && !token.isBlank(), errorCode);
        String key = prefix + token;
        String value = kvStore.get(key);
        Require.isTrue(value != null && kvStore.deleteIfValue(key, value), errorCode);
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            return Require.fail(errorCode, errorCode.getMessage(), exception);
        }
    }

    public record StatePayload(String tenantId, String appCode, ExternalAuthProvider provider,
                               ProviderAuthorizationIntent intent, String redirectUri, Long userId) {
    }

    public record BindingPayload(String tenantId, String appCode, ExternalAuthProvider provider,
                                 String providerTenantId, String externalUserId, String displayName) {
    }
}
