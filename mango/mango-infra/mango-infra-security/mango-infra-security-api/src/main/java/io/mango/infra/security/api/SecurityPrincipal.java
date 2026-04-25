package io.mango.infra.security.api;

/**
 * Authentication principal payload stored in Spring Security.
 *
 * @param userId authenticated subject ID
 * @param tenantId current tenant identifier
 * @param principalName authenticated principal name
 */
public record SecurityPrincipal(
        Long userId,
        String tenantId,
        String principalName) {
}
