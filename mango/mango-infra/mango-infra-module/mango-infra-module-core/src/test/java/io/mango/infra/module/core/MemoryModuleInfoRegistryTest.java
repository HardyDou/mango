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
}
