package io.mango.authorization.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 统一授权快照。
 */
@Schema(description = "统一授权快照")
public record AuthorizationSnapshot(
        @Schema(description = "角色编码集合") Set<String> roleCodes,
        @Schema(description = "权限码集合") Set<String> permissionCodes,
        @Schema(description = "授权标识集合") Set<String> authorities) {

    public AuthorizationSnapshot {
        roleCodes = immutable(roleCodes);
        permissionCodes = immutable(permissionCodes);
        authorities = immutable(authorities);
    }

    public static AuthorizationSnapshot empty() {
        return new AuthorizationSnapshot(Set.of(), Set.of(), Set.of());
    }

    public static AuthorizationSnapshot of(Collection<String> roleCodes,
                                           Collection<String> permissionCodes,
                                           Collection<String> authorities) {
        return new AuthorizationSnapshot(toSet(roleCodes), toSet(permissionCodes), toSet(authorities));
    }

    public AuthorizationSnapshot merge(AuthorizationSnapshot other) {
        if (other == null) {
            return this;
        }
        LinkedHashSet<String> mergedRoles = new LinkedHashSet<>(roleCodes);
        mergedRoles.addAll(other.roleCodes);
        LinkedHashSet<String> mergedPermissions = new LinkedHashSet<>(permissionCodes);
        mergedPermissions.addAll(other.permissionCodes);
        LinkedHashSet<String> mergedAuthorities = new LinkedHashSet<>(authorities);
        mergedAuthorities.addAll(other.authorities);
        return new AuthorizationSnapshot(mergedRoles, mergedPermissions, mergedAuthorities);
    }

    public boolean hasAuthority(String authority) {
        return authority != null && authorities.contains(authority);
    }

    private static Set<String> immutable(Collection<String> values) {
        return Collections.unmodifiableSet(toSet(values));
    }

    private static LinkedHashSet<String> toSet(Collection<String> values) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (values == null) {
            return normalized;
        }
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }
}
