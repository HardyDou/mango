package io.mango.authorization.core.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Stable primary keys for rows created from portable Authorization resources. */
public final class AuthorizationResourceIds {

    private AuthorizationResourceIds() {
    }

    public static long stable(String targetTable, Object... identityParts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, targetTable);
            for (Object identityPart : identityParts) {
                update(digest, String.valueOf(identityPart));
            }
            byte[] hash = digest.digest();
            long id = 0L;
            for (int i = 0; i < Long.BYTES; i++) {
                id = (id << Byte.SIZE) | (hash[i] & 0xffL);
            }
            id &= Long.MAX_VALUE;
            return id == 0L ? 1L : id;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    public static long declaredOrStable(Long declaredId, String targetTable, Object... identityParts) {
        if (declaredId == null) {
            return stable(targetTable, identityParts);
        }
        if (declaredId <= 0L) {
            throw new IllegalArgumentException("Authorization Resource targetId must be positive");
        }
        return declaredId;
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }
}
