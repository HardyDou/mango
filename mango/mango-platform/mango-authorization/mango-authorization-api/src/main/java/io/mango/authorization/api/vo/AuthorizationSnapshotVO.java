package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 统一授权快照。 */
@Data
@NoArgsConstructor
@Schema(description = "统一授权快照")
public class AuthorizationSnapshotVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色编码集合")
    private Set<String> roleCodes = Set.of();

    @Schema(description = "权限编码集合")
    private Set<String> permissionCodes = Set.of();

    @Schema(description = "Spring Security 权限集合")
    private Set<String> authorities = Set.of();

    @Schema(description = "按钮显示规则列表")
    private List<ButtonDisplayRuleVO> buttonRules = List.of();

    private AuthorizationSnapshotVO(Collection<String> roleCodes,
                                    Collection<String> permissionCodes,
                                    Collection<String> authorities,
                                    Collection<ButtonDisplayRuleVO> buttonRules) {
        this.roleCodes = immutableSet(roleCodes);
        this.permissionCodes = immutableSet(permissionCodes);
        this.authorities = immutableSet(authorities);
        this.buttonRules = immutableRules(buttonRules);
    }

    public static AuthorizationSnapshotVO empty() {
        return new AuthorizationSnapshotVO(Set.of(), Set.of(), Set.of(), List.of());
    }

    public static AuthorizationSnapshotVO of(Collection<String> roleCodes,
                                             Collection<String> permissionCodes,
                                             Collection<String> authorities) {
        return of(roleCodes, permissionCodes, authorities, List.of());
    }

    public static AuthorizationSnapshotVO of(Collection<String> roleCodes,
                                             Collection<String> permissionCodes,
                                             Collection<String> authorities,
                                             Collection<ButtonDisplayRuleVO> buttonRules) {
        return new AuthorizationSnapshotVO(roleCodes, permissionCodes, authorities, buttonRules);
    }

    public AuthorizationSnapshotVO merge(AuthorizationSnapshotVO other) {
        if (other == null) {
            return this;
        }
        LinkedHashSet<String> mergedRoles = new LinkedHashSet<>(safeSet(roleCodes));
        mergedRoles.addAll(safeSet(other.roleCodes));
        LinkedHashSet<String> mergedPermissions = new LinkedHashSet<>(safeSet(permissionCodes));
        mergedPermissions.addAll(safeSet(other.permissionCodes));
        LinkedHashSet<String> mergedAuthorities = new LinkedHashSet<>(safeSet(authorities));
        mergedAuthorities.addAll(safeSet(other.authorities));
        LinkedHashSet<ButtonDisplayRuleVO> mergedRules = new LinkedHashSet<>(safeRules(buttonRules));
        mergedRules.addAll(safeRules(other.buttonRules));
        return of(mergedRoles, mergedPermissions, mergedAuthorities, mergedRules);
    }

    public boolean hasAuthority(String authority) {
        return authority != null && safeSet(authorities).contains(authority);
    }

    public Set<String> roleCodes() { return safeSet(roleCodes); }
    public Set<String> permissionCodes() { return safeSet(permissionCodes); }
    public Set<String> authorities() { return safeSet(authorities); }
    public List<ButtonDisplayRuleVO> buttonRules() { return safeRules(buttonRules); }

    private static Set<String> immutableSet(Collection<String> values) {
        return Collections.unmodifiableSet(normalize(values));
    }

    private static List<ButtonDisplayRuleVO> immutableRules(Collection<ButtonDisplayRuleVO> values) {
        return Collections.unmodifiableList(normalizeRules(values));
    }

    private static Set<String> safeSet(Collection<String> values) {
        return values == null ? Set.of() : immutableSet(values);
    }

    private static List<ButtonDisplayRuleVO> safeRules(Collection<ButtonDisplayRuleVO> values) {
        return values == null ? List.of() : immutableRules(values);
    }

    private static LinkedHashSet<String> normalize(Collection<String> values) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (values == null) {
            return normalized;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.trim());
            }
        }
        return normalized;
    }

    private static List<ButtonDisplayRuleVO> normalizeRules(Collection<ButtonDisplayRuleVO> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(rule -> rule != null && rule.getCode() != null && !rule.getCode().isBlank())
                .toList();
    }
}
