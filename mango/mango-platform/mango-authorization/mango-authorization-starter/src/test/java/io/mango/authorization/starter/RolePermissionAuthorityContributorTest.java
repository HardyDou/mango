package io.mango.authorization.starter;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.core.service.ISubjectAuthorityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

@DisplayName("RolePermissionAuthorityContributor Tests")
class RolePermissionAuthorityContributorTest {

    @Test
    @DisplayName("supports tenant member subject type only")
    void supportsTenantMemberSubjectTypeOnly() {
        RolePermissionAuthorityContributor contributor =
                new RolePermissionAuthorityContributor(mock(ISubjectAuthorityService.class));

        assertTrue(contributor.supports(AuthorizationQuery.member(1L)));
        assertFalse(contributor.supports(new AuthorizationQuery(1L, "service", null, null)));
    }

    @Test
    @DisplayName("contribute should merge roles and permissions into authorities")
    void contributeShouldMergeRolesAndPermissions() {
        ISubjectAuthorityService subjectAuthorityService = mock(ISubjectAuthorityService.class);
        when(subjectAuthorityService.listSubjectRoles(any(AuthorizationQuery.class))).thenReturn(List.of("ROLE_ADMIN"));
        when(subjectAuthorityService.listSubjectPermissions(any(AuthorizationQuery.class))).thenReturn(List.of("system:user:view"));

        RolePermissionAuthorityContributor contributor = new RolePermissionAuthorityContributor(subjectAuthorityService);
        var snapshot = contributor.contribute(AuthorizationQuery.member(1L));

        assertEquals(List.of("ROLE_ADMIN"), snapshot.roleCodes().stream().toList());
        assertEquals(List.of("system:user:view"), snapshot.permissionCodes().stream().toList());
        assertTrue(snapshot.authorities().contains("ROLE_ADMIN"));
        assertTrue(snapshot.authorities().contains("system:user:view"));
    }
}
