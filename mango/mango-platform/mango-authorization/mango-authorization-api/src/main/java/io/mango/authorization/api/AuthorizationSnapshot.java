package io.mango.authorization.api;

import io.mango.authorization.api.vo.ButtonDisplayRuleVO;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 统一授权快照。
 */
public record AuthorizationSnapshot(
        Set<String> roleCodes,
        Set<String> permissionCodes,
        Set<String> authorities,
        List<ButtonDisplayRuleVO> buttonRules) {

    public AuthorizationSnapshot {
        roleCodes = immutable(roleCodes);
        permissionCodes = immutable(permissionCodes);
        authorities = immutable(authorities);
        buttonRules = immutableRules(buttonRules);
    }

    public static AuthorizationSnapshot empty() {
        return new AuthorizationSnapshot(Set.of(), Set.of(), Set.of(), List.of());
    }

    public static AuthorizationSnapshot of(Collection<String> roleCodes,
                                           Collection<String> permissionCodes,
                                           Collection<String> authorities) {
        return of(roleCodes, permissionCodes, authorities, List.of());
    }

    public static AuthorizationSnapshot of(Collection<String> roleCodes,
                                           Collection<String> permissionCodes,
                                           Collection<String> authorities,
                                           Collection<ButtonDisplayRuleVO> buttonRules) {
        return new AuthorizationSnapshot(
                toSet(roleCodes),
                toSet(permissionCodes),
                toSet(authorities),
                toRuleList(buttonRules));
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
        LinkedHashSet<ButtonDisplayRuleVO> mergedRules = new LinkedHashSet<>(buttonRules);
        mergedRules.addAll(other.buttonRules);
        return new AuthorizationSnapshot(mergedRoles, mergedPermissions, mergedAuthorities, mergedRules.stream().toList());
    }

    public boolean hasAuthority(String authority) {
        return authority != null && authorities.contains(authority);
    }

    private static Set<String> immutable(Collection<String> values) {
        return Collections.unmodifiableSet(toSet(values));
    }

    private static List<ButtonDisplayRuleVO> immutableRules(Collection<ButtonDisplayRuleVO> values) {
        return Collections.unmodifiableList(toRuleList(values));
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

    private static List<ButtonDisplayRuleVO> toRuleList(Collection<ButtonDisplayRuleVO> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(rule -> rule != null && rule.getCode() != null && !rule.getCode().isBlank())
                .toList();
    }
}
