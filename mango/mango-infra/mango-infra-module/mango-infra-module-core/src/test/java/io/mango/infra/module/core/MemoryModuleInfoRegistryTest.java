package io.mango.infra.module.core;

import io.mango.infra.module.api.ModuleInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryModuleInfoRegistryTest {

    @Test
    void resolve_registeredModule_returnsModuleInfo() {
        MemoryModuleInfoRegistry registry = new MemoryModuleInfoRegistry();
        registry.register(new ModuleInfo("mango-rbac", "mango-admin-app", "/admin", "/rbac", "test"));

        var moduleInfo = registry.resolve("mango-rbac");

        assertTrue(moduleInfo.isPresent());
        assertEquals("mango-admin-app", moduleInfo.get().serviceName());
        assertEquals("/admin", moduleInfo.get().contextPath());
        assertEquals("/rbac", moduleInfo.get().modulePath());
    }

    @Test
    void resolveByRequestPath_whenPathMatchesModulePath_returnsModuleInfo() {
        MemoryModuleInfoRegistry registry = new MemoryModuleInfoRegistry();
        registry.register(new ModuleInfo("mango-system", "mango-platform-app", "/", "/system", "test"));
        registry.register(new ModuleInfo("mango-system-config", "mango-platform-app", "/", "/system/config", "test"));

        var moduleInfo = registry.resolveByRequestPath("/system/config/list");

        assertTrue(moduleInfo.isPresent());
        assertEquals("mango-system-config", moduleInfo.get().moduleName());
    }

    @Test
    void resolveByRequestPath_whenPathIncludesContextPath_returnsModuleInfo() {
        MemoryModuleInfoRegistry registry = new MemoryModuleInfoRegistry();
        registry.register(new ModuleInfo("mango-authorization", "mango-platform-app", "/platform", "/authorization", "test"));

        var moduleInfo = registry.resolveByRequestPath("/platform/authorization/roles");

        assertTrue(moduleInfo.isPresent());
        assertEquals("mango-authorization", moduleInfo.get().moduleName());
        assertEquals("/platform/authorization", moduleInfo.get().runtimeBasePath());
    }
}
