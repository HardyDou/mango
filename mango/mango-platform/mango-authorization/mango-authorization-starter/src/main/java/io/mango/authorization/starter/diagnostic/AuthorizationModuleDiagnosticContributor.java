package io.mango.authorization.starter.diagnostic;

import io.mango.authorization.core.entity.ApiResourceEntity;
import io.mango.authorization.core.entity.MenuEntity;
import io.mango.authorization.core.mapper.ApiResourceMapper;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.infra.module.api.diagnostic.ModuleConditionStatus;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticCondition;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticContributor;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticProfile;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticRequest;
import io.mango.infra.module.api.diagnostic.ModuleInstallation;
import io.mango.resource.api.ResourceAuthorizationRequirementsProvider;
import io.mango.resource.api.vo.ApiRequirementVO;
import io.mango.resource.api.vo.AuthorizationRequirementsVO;
import io.mango.resource.api.vo.MenuRequirementVO;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** Cross-checks current Resource declarations against Authorization materialized rows. */
@Slf4j
public class AuthorizationModuleDiagnosticContributor implements ModuleDiagnosticContributor {

    private static final long NANOS_PER_MILLISECOND = TimeUnit.MILLISECONDS.toNanos(1);
    private static final String SUPPORTED_MODULE = "mango-link";
    private static final String PLATFORM_MENU_TENANT = "1";
    private static final String GLOBAL_API_RESOURCE_TENANT = "default";

    private final ResourceAuthorizationRequirementsProvider requirementsProvider;
    private final MenuMapper menuMapper;
    private final ApiResourceMapper apiResourceMapper;

    public AuthorizationModuleDiagnosticContributor(
            ResourceAuthorizationRequirementsProvider requirementsProvider,
            MenuMapper menuMapper,
            ApiResourceMapper apiResourceMapper) {
        this.requirementsProvider = requirementsProvider;
        this.menuMapper = menuMapper;
        this.apiResourceMapper = apiResourceMapper;
    }

    @Override
    public Collection<ModuleDiagnosticCondition> contribute(ModuleDiagnosticRequest request) {
        Instant startedAt = Instant.now();
        long startedNanos = System.nanoTime();
        if (!SUPPORTED_MODULE.equals(request.moduleCode())) {
            return List.of(condition(
                    ModuleConditionStatus.UNKNOWN,
                    "MODULE_NOT_SUPPORTED",
                    Map.of("supportedModule", SUPPORTED_MODULE),
                    startedAt,
                    startedNanos));
        }
        String resourceModule = request.attributes().get(ModuleInstallation.RESOURCE_MODULE_ATTRIBUTE);
        if (resourceModule == null || resourceModule.isBlank()) {
            return List.of(condition(
                    ModuleConditionStatus.UNKNOWN,
                    "MAPPING_UNRESOLVED",
                    Map.of(),
                    startedAt,
                    startedNanos));
        }
        AuthorizationRequirementsVO requirements = requirementsProvider.authorizationRequirements(
                resourceModule, request.moduleCode(), request.appCode());
        if (requirements.menus().isEmpty() || requirements.apis().isEmpty()) {
            return List.of(condition(
                    ModuleConditionStatus.UNKNOWN,
                    "CURRENT_REQUIREMENTS_NOT_OBSERVED",
                    safeEvidence(requirements, requirements.menus().size(), requirements.apis().size()),
                    startedAt,
                    startedNanos));
        }
        if (!requirements.sourcesApplied()) {
            return List.of(condition(
                    ModuleConditionStatus.UNKNOWN,
                    "RESOURCE_REQUIREMENTS_NOT_APPLIED",
                    safeEvidence(requirements, requirements.menus().size(), requirements.apis().size()),
                    startedAt,
                    startedNanos));
        }
        if (menuMapper == null || apiResourceMapper == null) {
            return List.of(condition(
                    ModuleConditionStatus.UNKNOWN,
                    "AUTHORIZATION_DIAGNOSTIC_DEPENDENCY_MISSING",
                    safeEvidence(requirements, requirements.menus().size(), requirements.apis().size()),
                    startedAt,
                    startedNanos));
        }

        try {
            MaterializationCounts counts = compare(requirements, request.appCode());
            ModuleConditionStatus status = counts.missingMenus() == 0 && counts.missingApis() == 0
                    ? ModuleConditionStatus.PASS
                    : ModuleConditionStatus.FAIL;
            String reasonCode = status == ModuleConditionStatus.PASS
                    ? "AUTHORIZATION_MATERIALIZED"
                    : "AUTHORIZATION_MENU_API_MISMATCH";
            return List.of(condition(
                    status,
                    reasonCode,
                    safeEvidence(requirements, counts.missingMenus(), counts.missingApis()),
                    startedAt,
                    startedNanos));
        } catch (RuntimeException queryFailure) {
            log.warn("Mango Authorization module diagnostic query failed: module={}",
                    request.moduleCode(), queryFailure);
            return List.of(condition(
                    ModuleConditionStatus.UNKNOWN,
                    "AUTHORIZATION_DIAGNOSTIC_QUERY_FAILED",
                    safeEvidence(requirements, requirements.menus().size(), requirements.apis().size()),
                    startedAt,
                    startedNanos));
        }
    }

    private MaterializationCounts compare(AuthorizationRequirementsVO requirements, String appCode) {
        List<String> menuCodes = requirements.menus().stream()
                .map(MenuRequirementVO::menuCode)
                .distinct()
                .toList();
        Map<MenuKey, MenuEntity> actualMenus = menuMapper.selectDiagnosticMenus(
                        PLATFORM_MENU_TENANT, appCode, menuCodes)
                .stream()
                .collect(Collectors.toMap(MenuKey::from, item -> item, (left, right) -> left));
        int missingMenus = 0;
        for (MenuRequirementVO expected : requirements.menus()) {
            MenuEntity actual = actualMenus.get(MenuKey.from(expected));
            if (actual == null
                    || !Objects.equals(normalize(expected.component()), normalize(actual.getComponent()))
                    || !Objects.equals(expected.status(), actual.getStatus())
                    || !normalizeCodes(expected.apiCodes()).equals(normalizeCodes(actual.getApiCodes()))) {
                missingMenus++;
            }
        }

        Map<ApiKey, ApiResourceEntity> actualApis = apiResourceMapper.selectDiagnosticApis(
                        GLOBAL_API_RESOURCE_TENANT, requirements.apis().getFirst().moduleName())
                .stream()
                .collect(Collectors.toMap(ApiKey::from, item -> item, (left, right) -> left));
        int missingApis = 0;
        for (ApiRequirementVO expected : requirements.apis()) {
            ApiResourceEntity actual = actualApis.get(ApiKey.from(expected));
            if (actual == null
                    || !Objects.equals(normalize(expected.resourceCode()), normalize(actual.getResourceCode()))
                    || !Objects.equals(normalize(expected.permissionCode()), normalize(actual.getPermissionCode()))
                    || !Objects.equals(normalize(expected.accessMode()), normalize(actual.getAccessMode()))
                    || !Objects.equals(expected.status(), actual.getStatus())) {
                missingApis++;
            }
        }
        return new MaterializationCounts(missingMenus, missingApis);
    }

    private Map<String, Object> safeEvidence(
            AuthorizationRequirementsVO requirements,
            int missingMenus,
            int missingApis) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("expectedMenuCount", requirements.menus().size());
        evidence.put("missingMenuCount", missingMenus);
        evidence.put("expectedApiCount", requirements.apis().size());
        evidence.put("missingApiCount", missingApis);
        evidence.put("pageRequirements", requirements.menus().stream()
                .map(MenuRequirementVO::component)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList());
        return evidence;
    }

    private ModuleDiagnosticCondition condition(
            ModuleConditionStatus status,
            String reasonCode,
            Map<String, Object> evidence,
            Instant observedAt,
            long startedNanos) {
        return new ModuleDiagnosticCondition(
                ModuleDiagnosticProfile.AUTHORIZATION_MENU_API,
                status,
                true,
                reasonCode,
                evidence,
                observedAt,
                (System.nanoTime() - startedNanos) / NANOS_PER_MILLISECOND,
                false);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<String> normalizeCodes(List<String> values) {
        return values == null ? List.of() : values.stream()
                .map(this::normalize)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> normalizeCodes(String value) {
        return value == null ? List.of() : normalizeCodes(Arrays.asList(value.split(",")));
    }

    private record MaterializationCounts(int missingMenus, int missingApis) {
    }

    private record MenuKey(String appCode, String moduleCode, String menuCode) {

        private static MenuKey from(MenuRequirementVO requirement) {
            return new MenuKey(requirement.appCode(), requirement.moduleCode(), requirement.menuCode());
        }

        private static MenuKey from(MenuEntity entity) {
            return new MenuKey(entity.getAppCode(), entity.getModuleCode(), entity.getMenuCode());
        }
    }

    private record ApiKey(String moduleName, String httpMethod, String pathPattern) {

        private static ApiKey from(ApiRequirementVO requirement) {
            return new ApiKey(requirement.moduleName(), requirement.httpMethod(), requirement.pathPattern());
        }

        private static ApiKey from(ApiResourceEntity entity) {
            return new ApiKey(entity.getModuleName(), entity.getHttpMethod(), entity.getPathPattern());
        }
    }
}
