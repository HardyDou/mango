package io.mango.resource.api;

import io.mango.common.contract.LocalCapabilityContract;

import java.util.List;

/**
 * Process-local current Resource declarations used by Authorization materialization diagnostics.
 * API paths in this contract are internal facts and must not be copied into endpoint evidence.
 */
@LocalCapabilityContract
public interface ResourceAuthorizationRequirementsProvider {

    AuthorizationRequirements authorizationRequirements(String resourceModule, String runtimeModule, String appCode);

    record AuthorizationRequirements(
            List<MenuRequirement> menus,
            List<ApiRequirement> apis,
            boolean sourcesApplied) {

        public AuthorizationRequirements {
            menus = menus == null ? List.of() : List.copyOf(menus);
            apis = apis == null ? List.of() : List.copyOf(apis);
        }

        public static AuthorizationRequirements empty() {
            return new AuthorizationRequirements(List.of(), List.of(), false);
        }
    }

    record MenuRequirement(
            String appCode,
            String moduleCode,
            String menuCode,
            String component,
            List<String> apiCodes,
            int status) {

        public MenuRequirement {
            apiCodes = apiCodes == null ? List.of() : List.copyOf(apiCodes);
        }
    }

    record ApiRequirement(
            String moduleName,
            String httpMethod,
            String pathPattern,
            String resourceCode,
            String permissionCode,
            String accessMode,
            int status) {
    }
}
