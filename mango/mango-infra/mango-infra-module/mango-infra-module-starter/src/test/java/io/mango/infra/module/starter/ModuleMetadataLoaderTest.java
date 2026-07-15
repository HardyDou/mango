package io.mango.infra.module.starter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ModuleMetadataLoaderTest {

    @Test
    void load_withoutThreadContextClassLoader_usesDefiningClassLoader() {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(null);

            assertDoesNotThrow(() -> new ModuleMetadataLoader().load());
        } finally {
            thread.setContextClassLoader(previous);
        }
    }
}
