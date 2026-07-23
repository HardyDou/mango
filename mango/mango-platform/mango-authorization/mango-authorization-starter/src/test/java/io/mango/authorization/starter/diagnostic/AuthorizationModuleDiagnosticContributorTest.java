package io.mango.authorization.starter.diagnostic;

import io.mango.authorization.core.entity.ApiResourceEntity;
import io.mango.authorization.core.entity.MenuEntity;
import io.mango.authorization.diagnostic.AuthorizationDiagnosticMapper;
import io.mango.infra.module.api.diagnostic.ModuleConditionStatus;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticCondition;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticProfile;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticRequest;
import io.mango.infra.module.api.diagnostic.ModuleInstallation;
import io.mango.resource.api.ResourceAuthorizationRequirementsProvider;
import io.mango.resource.api.ResourceAuthorizationRequirementsProvider.ApiRequirement;
import io.mango.resource.api.ResourceAuthorizationRequirementsProvider.AuthorizationRequirements;
import io.mango.resource.api.ResourceAuthorizationRequirementsProvider.MenuRequirement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorizationModuleDiagnosticContributorTest {

    private final ResourceAuthorizationRequirementsProvider requirementsProvider =
            mock(ResourceAuthorizationRequirementsProvider.class);
    private final AuthorizationDiagnosticMapper diagnosticMapper = mock(AuthorizationDiagnosticMapper.class);
    private final AuthorizationModuleDiagnosticContributor contributor =
            new AuthorizationModuleDiagnosticContributor(requirementsProvider, diagnosticMapper);

    @Test
    void matchingCurrentMenuAndApiRowsPassWithRedactedEvidence() {
        AuthorizationRequirements requirements = requirements(true);
        when(requirementsProvider.authorizationRequirements("link", "mango-link", "internal-admin"))
                .thenReturn(requirements);
        when(diagnosticMapper.selectMenus(any(), any(), any()))
                .thenReturn(requirements.menus().stream().map(this::menu).toList());
        when(diagnosticMapper.selectApis(any(), any()))
                .thenReturn(requirements.apis().stream().map(this::api).toList());

        ModuleDiagnosticCondition condition = diagnose();

        assertThat(condition.status()).isEqualTo(ModuleConditionStatus.PASS);
        assertThat(condition.reasonCode()).isEqualTo("AUTHORIZATION_MATERIALIZED");
        assertThat(condition.evidence()).containsEntry("expectedMenuCount", 2);
        assertThat(condition.evidence()).containsEntry("missingMenuCount", 0);
        assertThat(condition.evidence()).containsEntry("expectedApiCount", 1);
        assertThat(condition.evidence()).containsEntry("missingApiCount", 0);
        assertThat(condition.evidence().toString())
                .doesNotContain("/link/items")
                .doesNotContain("handler")
                .doesNotContain("tenant");
        verify(diagnosticMapper).selectApis("default", "mango-link");
    }

    @Test
    void missingMaterializedRowsFailInsteadOfTrustingResourceSuccess() {
        AuthorizationRequirements requirements = requirements(true);
        when(requirementsProvider.authorizationRequirements("link", "mango-link", "internal-admin"))
                .thenReturn(requirements);
        when(diagnosticMapper.selectMenus(any(), any(), any()))
                .thenReturn(List.of(menu(requirements.menus().get(0))));
        when(diagnosticMapper.selectApis(any(), any())).thenReturn(List.of());

        ModuleDiagnosticCondition condition = diagnose();

        assertThat(condition.status()).isEqualTo(ModuleConditionStatus.FAIL);
        assertThat(condition.reasonCode()).isEqualTo("AUTHORIZATION_MENU_API_MISMATCH");
        assertThat(condition.evidence()).containsEntry("missingMenuCount", 1);
        assertThat(condition.evidence()).containsEntry("missingApiCount", 1);
    }

    @Test
    void unappliedOrIncompleteCurrentRequirementsRemainUnknownWithoutDatabaseReads() {
        when(requirementsProvider.authorizationRequirements("link", "mango-link", "internal-admin"))
                .thenReturn(requirements(false));

        ModuleDiagnosticCondition unapplied = diagnose();

        assertThat(unapplied.status()).isEqualTo(ModuleConditionStatus.UNKNOWN);
        assertThat(unapplied.reasonCode()).isEqualTo("RESOURCE_REQUIREMENTS_NOT_APPLIED");
        verify(diagnosticMapper, never()).selectMenus(any(), any(), any());
        verify(diagnosticMapper, never()).selectApis(any(), any());

        when(requirementsProvider.authorizationRequirements("link", "mango-link", "internal-admin"))
                .thenReturn(AuthorizationRequirements.empty());
        ModuleDiagnosticCondition missing = diagnose();
        assertThat(missing.status()).isEqualTo(ModuleConditionStatus.UNKNOWN);
        assertThat(missing.reasonCode()).isEqualTo("CURRENT_REQUIREMENTS_NOT_OBSERVED");
    }

    @Test
    void wrongPermissionFieldsFailEvenWhenEndpointCoordinatesMatch() {
        AuthorizationRequirements requirements = requirements(true);
        when(requirementsProvider.authorizationRequirements("link", "mango-link", "internal-admin"))
                .thenReturn(requirements);
        when(diagnosticMapper.selectMenus(any(), any(), any()))
                .thenReturn(requirements.menus().stream().map(this::menu).toList());
        ApiResourceEntity api = api(requirements.apis().getFirst());
        api.setPermissionCode("wrong:permission");
        when(diagnosticMapper.selectApis(any(), any())).thenReturn(List.of(api));

        ModuleDiagnosticCondition condition = diagnose();

        assertThat(condition.status()).isEqualTo(ModuleConditionStatus.FAIL);
        assertThat(condition.reasonCode()).isEqualTo("AUTHORIZATION_MENU_API_MISMATCH");
        assertThat(condition.evidence()).containsEntry("missingApiCount", 1);
    }

    private ModuleDiagnosticCondition diagnose() {
        ModuleDiagnosticRequest request = new ModuleDiagnosticRequest(
                "mango-link",
                "internal-admin",
                ModuleDiagnosticProfile.ADMIN_MODULE_RUNTIME_V1,
                Map.of(ModuleInstallation.RESOURCE_MODULE_ATTRIBUTE, "link"));
        return contributor.contribute(request).iterator().next();
    }

    private AuthorizationRequirements requirements(boolean sourcesApplied) {
        return new AuthorizationRequirements(
                List.of(
                        new MenuRequirement(
                                "internal-admin", "mango-link", "link:navigation", null,
                                List.of("link:item:view"), 1),
                        new MenuRequirement(
                                "internal-admin", "mango-link", "data:link:item", "link/items/index",
                                List.of("link:item:view"), 1)),
                List.of(new ApiRequirement(
                        "mango-link", "GET", "/link/items", "GET:/link/items",
                        "link:item:view", "PERMISSION", 1)),
                sourcesApplied);
    }

    private MenuEntity menu(MenuRequirement requirement) {
        MenuEntity entity = new MenuEntity();
        entity.setAppCode(requirement.appCode());
        entity.setModuleCode(requirement.moduleCode());
        entity.setMenuCode(requirement.menuCode());
        entity.setComponent(requirement.component());
        entity.setApiCodes(String.join(",", requirement.apiCodes()));
        entity.setStatus(requirement.status());
        entity.setDelFlag(0);
        return entity;
    }

    private ApiResourceEntity api(ApiRequirement requirement) {
        ApiResourceEntity entity = new ApiResourceEntity();
        entity.setModuleName(requirement.moduleName());
        entity.setHttpMethod(requirement.httpMethod());
        entity.setPathPattern(requirement.pathPattern());
        entity.setResourceCode(requirement.resourceCode());
        entity.setPermissionCode(requirement.permissionCode());
        entity.setAccessMode(requirement.accessMode());
        entity.setStatus(requirement.status());
        entity.setDeleted(0);
        return entity;
    }
}
