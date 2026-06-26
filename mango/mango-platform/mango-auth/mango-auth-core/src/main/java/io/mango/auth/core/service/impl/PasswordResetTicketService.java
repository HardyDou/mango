package io.mango.auth.core.service.impl;

import io.mango.common.exception.BizException;
import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 强制改密一次性凭据服务。
 */
@Service
@RequiredArgsConstructor
public class PasswordResetTicketService {

    private static final String KEY_PREFIX = "auth:password-reset-ticket:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final IKvStore kvStore;

    @Value("${mango.auth.password-reset-ticket-ttl-seconds:600}")
    private long ticketTtlSeconds;

    public String issue(TicketPayload payload) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        kvStore.set(key(ticket), payload.serialize(), ticketTtlSeconds);
        return ticket;
    }

    public TicketPayload consume(String ticket) {
        TicketPayload payload = peek(ticket);
        revoke(ticket);
        return payload;
    }

    public TicketPayload peek(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new BizException(1417, "强制改密凭据无效或已过期");
        }
        return TicketPayload.deserialize(kvStore.get(key(ticket)));
    }

    public void revoke(String ticket) {
        if (ticket != null && !ticket.isBlank()) {
            kvStore.delete(key(ticket));
        }
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException(1417, "强制改密凭据无效或已过期");
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new BizException(1417, "强制改密凭据无效或已过期", e);
        }
    }

    private static Long parseNullableLong(String value) {
        return value == null || value.isBlank() ? null : parseLong(value);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String key(String ticket) {
        return KEY_PREFIX + ticket;
    }

    public record TicketPayload(Long userId,
                                String tenantId,
                                String tenantCode,
                                String appCode,
                                String realm,
                                String actorType,
                                String partyType,
                                Long partyId) {

        String serialize() {
            return value(userId)
                    + "|" + value(tenantId)
                    + "|" + value(tenantCode)
                    + "|" + value(appCode)
                    + "|" + value(realm)
                    + "|" + value(actorType)
                    + "|" + value(partyType)
                    + "|" + value(partyId);
        }

        static TicketPayload deserialize(String value) {
            if (value == null || value.isBlank()) {
                throw new BizException(1417, "强制改密凭据无效或已过期");
            }
            String[] parts = value.split("\\|", -1);
            if (parts.length != 8) {
                throw new BizException(1417, "强制改密凭据无效或已过期");
            }
            return new TicketPayload(parseLong(parts[0]), emptyToNull(parts[1]), emptyToNull(parts[2]),
                    emptyToNull(parts[3]), emptyToNull(parts[4]), emptyToNull(parts[5]), emptyToNull(parts[6]),
                    parseNullableLong(parts[7]));
        }

        private static String value(Object value) {
            return value == null ? "" : String.valueOf(value);
        }
    }
}
