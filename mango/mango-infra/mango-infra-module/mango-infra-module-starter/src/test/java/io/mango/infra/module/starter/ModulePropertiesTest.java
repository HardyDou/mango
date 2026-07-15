package io.mango.infra.module.starter;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModulePropertiesTest {

    @Test
    void modules_exposesReadOnlyDefensiveCollection() {
        ModuleProperties properties = new ModuleProperties();
        ModuleProperties.ModuleServiceProperties service = new ModuleProperties.ModuleServiceProperties();
        Map<String, ModuleProperties.ModuleServiceProperties> configured = new LinkedHashMap<>();
        configured.put("mango-system", service);

        properties.setModules(configured);
        configured.clear();

        assertEquals(1, properties.getModules().size());
        assertThrows(UnsupportedOperationException.class,
                () -> properties.getModules().clear());
    }
}
