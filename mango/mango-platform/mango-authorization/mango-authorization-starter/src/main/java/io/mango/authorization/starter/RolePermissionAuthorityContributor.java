package io.mango.authorization.starter;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.AuthorizationSnapshot;
import io.mango.authorization.api.AuthorityContributor;
import io.mango.authorization.core.service.ISubjectAuthorityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;

/**
 * 用户主体授权贡献者。
 */
@Component
@RequiredArgsConstructor
public class RolePermissionAuthorityContributor implements AuthorityContributor {

    private final ISubjectAuthorityService subjectAuthorityService;

    @Override
    public boolean supports(AuthorizationQuery query) {
        return AuthorizationQuery.SUBJECT_TYPE_USER.equals(query.subjectType());
    }

    @Override
    public AuthorizationSnapshot contribute(AuthorizationQuery query) {
        LinkedHashSet<String> roleCodes = new LinkedHashSet<>(
                subjectAuthorityService.listSubjectRoles(query));
        LinkedHashSet<String> permissionCodes = new LinkedHashSet<>(
                subjectAuthorityService.listSubjectPermissions(query));
        LinkedHashSet<String> authorities = new LinkedHashSet<>(roleCodes);
        authorities.addAll(permissionCodes);
        return AuthorizationSnapshot.of(roleCodes, permissionCodes, authorities);
    }
}
