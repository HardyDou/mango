package io.mango.infra.security.api;

/**
 * Immutable security context snapshot.
 *
 * @param userId authenticated subject ID
 * @param tenantId current tenant identifier
 * @param authenticated whether the current request is authenticated
 * @param principalName authenticated principal name
 */
public record SecurityContext(
        Long userId,
        String tenantId,
        boolean authenticated,
        String principalName) {

    /**
     * Anonymous context for unauthenticated or non-request execution paths.
     *
     * @return anonymous context
     */
    public static SecurityContext anonymous() {
        return new SecurityContext(null, null, false, null);
    }
}
