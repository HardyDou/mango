package io.mango.auth.core.store;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.auth.api.enums.AuthCode;
import io.mango.common.result.Require;
import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 强制改密一次性凭据服务。
 */
@Component
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "The KV store is an intentionally shared Spring infrastructure collaborator")
public class PasswordResetTicketStore {

    private static final String KEY_PREFIX = "auth:password-reset-ticket:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TICKET_RANDOM_BYTES = 32;
    private static final int PAYLOAD_PART_COUNT = 8;
    private static final int USER_ID_INDEX = 0;
    private static final int TENANT_ID_INDEX = 1;
    private static final int TENANT_CODE_INDEX = 2;
    private static final int APP_CODE_INDEX = 3;
    private static final int REALM_INDEX = 4;
    private static final int ACTOR_TYPE_INDEX = 5;
    private static final int PARTY_TYPE_INDEX = 6;
    private static final int PARTY_ID_INDEX = 7;

    private final IKvStore kvStore;

    @Value("${mango.auth.password-reset-ticket-ttl-seconds:600}")
    private long ticketTtlSeconds;

    public String issue(TicketPayload payload) {
        byte[] bytes = new byte[TICKET_RANDOM_BYTES];
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
            Require.fail(AuthCode.PASSWORD_RESET_TICKET_INVALID);
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
            Require.fail(AuthCode.PASSWORD_RESET_TICKET_INVALID);
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return Require.fail(AuthCode.PASSWORD_RESET_TICKET_INVALID, AuthCode.PASSWORD_RESET_TICKET_INVALID.getMessage(), e);
        }
    }

    private static Long parseNullableLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseLong(value);
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
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
                return Require.fail(AuthCode.PASSWORD_RESET_TICKET_INVALID);
            }
            String[] parts = value.split("\\|", -1);
            if (parts.length != PAYLOAD_PART_COUNT) {
                return Require.fail(AuthCode.PASSWORD_RESET_TICKET_INVALID);
            }
            return new TicketPayload(
                    parseLong(parts[USER_ID_INDEX]),
                    emptyToNull(parts[TENANT_ID_INDEX]),
                    emptyToNull(parts[TENANT_CODE_INDEX]),
                    emptyToNull(parts[APP_CODE_INDEX]),
                    emptyToNull(parts[REALM_INDEX]),
                    emptyToNull(parts[ACTOR_TYPE_INDEX]),
                    emptyToNull(parts[PARTY_TYPE_INDEX]),
                    parseNullableLong(parts[PARTY_ID_INDEX]));
        }

        private static String value(Object value) {
            if (value == null) {
                return "";
            }
            return String.valueOf(value);
        }
    }
}
