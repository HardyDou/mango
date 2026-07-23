package io.mango.resource.api;

import io.mango.resource.api.vo.AuthorizationRequirementsVO;

/**
 * Process-local current Resource declarations used by Authorization materialization diagnostics.
 * API paths in this contract are internal facts and must not be copied into endpoint evidence.
 */
public interface ResourceAuthorizationRequirementsProvider {

    AuthorizationRequirementsVO authorizationRequirements(
            String resourceModule,
            String runtimeModule,
            String appCode);
}
