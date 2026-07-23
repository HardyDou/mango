package io.mango.authorization.starter.diagnostic;

import io.mango.authorization.core.entity.ApiResourceEntity;
import io.mango.authorization.core.entity.MenuEntity;
import io.mango.authorization.core.mapper.ApiResourceMapper;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.infra.module.api.diagnostic.ModuleConditionStatus;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticCondition;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticProfile;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticRequest;
import io.mango.infra.module.api.diagnostic.ModuleInstallation;
import io.mango.resource.api.ResourceAuthorizationRequirementsProvider;
import io.mango.resource.api.vo.ApiRequirementVO;
import io.mango.resource.api.vo.AuthorizationRequirementsVO;
import io.mango.resource.api.vo.MenuRequirementVO;
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
    private final MenuMapper menuMapper = mock(MenuMapper.class);
    private final ApiResourceMapper apiResourceMapper = mock(ApiResourceMapper.class);
    private final AuthorizationModuleDiagnosticContributor contributor =
            new AuthorizationModuleDiagnosticContributor(requirementsProvider, menuMapper, apiResourceMapper);

    @Test
    void matchingCurrentMenuAndApiRowsPassWithRedactedEvidence() {
        AuthorizationRequirementsVO requirements = requirements(true);
        when(requirementsProvider.authorizationRequirements("link", "mango-link", "internal-admin"))
                .thenReturn(requirements);
        when(menuMapper.selectDiagnosticMenus(any(), any(), any()))
                .thenReturn(requirements.menus().stream().map(this::menu).toList());
        when(apiResourceMapper.selectDiagnosticApis(any(), any()))
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
        verify(apiResourceMapper).selectDiagnosticApis("default", "mango-link");
    }

    @Test
    void missingMaterializedRowsFailInsteadOfTrustingResourceSuccess() {
        AuthorizationRequirementsVO requirements = requirements(true);
        when(requirementsProvider.authorizationRequirements("link", "mango-link", "internal-admin"))
                .thenReturn(requirements);
        when(menuMapper.selectDiagnosticMenus(any(), any(), any()))
                .thenReturn(List.of(menu(requirements.menus().get(0))));
        when(apiResourceMapper.selectDiagnosticApis(any(), any())).thenReturn(List.of());

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
        verify(menuMapper, never()).selectDiagnosticMenus(any(), any(), any());
        verify(apiResourceMapper, never()).selectDiagnosticApis(any(), any());

        when(requirementsProvider.authorizationRequirements("link", "mango-link", "internal-admin"))
                .thenReturn(AuthorizationRequirementsVO.empty());
        ModuleDiagnosticCondition missing = diagnose();
        assertThat(missing.status()).isEqualTo(ModuleConditionStatus.UNKNOWN);
        assertThat(missing.reasonCode()).isEqualTo("CURRENT_REQUIREMENTS_NOT_OBSERVED");
    }

    @Test
    void wrongPermissionFieldsFailEvenWhenEndpointCoordinatesMatch() {
        AuthorizationRequirementsVO requirements = requirements(true);
        when(requirementsProvider.authorizationRequirements("link", "mango-link", "internal-admin"))
                .thenReturn(requirements);
        when(menuMapper.selectDiagnosticMenus(any(), any(), any()))
                .thenReturn(requirements.menus().stream().map(this::menu).toList());
        ApiResourceEntity api = api(requirements.apis().getFirst());
        api.setPermissionCode("wrong:permission");
        when(apiResourceMapper.selectDiagnosticApis(any(), any())).thenReturn(List.of(api));

        ModuleDiagnosticCondition condition = diagnose();

        assertThat(condition.status()).isEqualTo(ModuleConditionStatus.FAIL);
        assertThat(condition.reasonCode()).isEqualTo("AUTHORIZATION_MENU_API_MISMATCH");
        assertThat(condition.evidence()).containsEntry("missingApiCount", 1);
    }

    @Test
    void missingMapperDependencyRemainsUnknownInsteadOfReportingReady() {
        AuthorizationRequirementsVO requirements = requirements(true);
        when(requirementsProvider.authorizationRequirements("link", "mango-link", "internal-admin"))
                .thenReturn(requirements);
        AuthorizationModuleDiagnosticContributor unavailableContributor =
                new AuthorizationModuleDiagnosticContributor(requirementsProvider, null, null);

        ModuleDiagnosticCondition condition = unavailableContributor.contribute(new ModuleDiagnosticRequest(
                "mango-link",
                "internal-admin",
                ModuleDiagnosticProfile.ADMIN_MODULE_RUNTIME_V1,
                Map.of(ModuleInstallation.RESOURCE_MODULE_ATTRIBUTE, "link"))).iterator().next();

        assertThat(condition.status()).isEqualTo(ModuleConditionStatus.UNKNOWN);
        assertThat(condition.reasonCode()).isEqualTo("AUTHORIZATION_DIAGNOSTIC_DEPENDENCY_MISSING");
    }

    private ModuleDiagnosticCondition diagnose() {
        ModuleDiagnosticRequest request = new ModuleDiagnosticRequest(
                "mango-link",
                "internal-admin",
                ModuleDiagnosticProfile.ADMIN_MODULE_RUNTIME_V1,
                Map.of(ModuleInstallation.RESOURCE_MODULE_ATTRIBUTE, "link"));
        return contributor.contribute(request).iterator().next();
    }

    private AuthorizationRequirementsVO requirements(boolean sourcesApplied) {
        return new AuthorizationRequirementsVO(
                List.of(
                        new MenuRequirementVO(
                                "internal-admin", "mango-link", "link:navigation", null,
                                List.of("link:item:view"), 1),
                        new MenuRequirementVO(
                                "internal-admin", "mango-link", "data:link:item", "link/items/index",
                                List.of("link:item:view"), 1)),
                List.of(new ApiRequirementVO(
                        "mango-link", "GET", "/link/items", "GET:/link/items",
                        "link:item:view", "PERMISSION", 1)),
                sourcesApplied);
    }

    private MenuEntity menu(MenuRequirementVO requirement) {
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

    private ApiResourceEntity api(ApiRequirementVO requirement) {
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
