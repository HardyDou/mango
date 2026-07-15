package io.mango.infra.module.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleInfoTest {

    @Test
    void paths_withTrailingSeparators_normalizeToStableRuntimePath() {
        ModuleInfo moduleInfo = new ModuleInfo(
                "mango-rbac", "mango-admin-app", " /admin/ ", " /rbac/ ", "test");

        assertEquals("/admin", moduleInfo.contextPath());
        assertEquals("/rbac", moduleInfo.modulePath());
        assertEquals("/admin/rbac", moduleInfo.runtimeBasePath());
        assertTrue(moduleInfo.matchesRequestPath("/admin/rbac/users"));
    }

    @Test
    void rootModulePath_matchesNestedRequestPath() {
        ModuleInfo moduleInfo = new ModuleInfo(
                "mango-root", "mango-root-app", "", "/", "test");

        assertTrue(moduleInfo.matchesRequestPath("/health/readiness"));
    }
}
