package io.mango.dal.starter.remote;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DalStoreFeignClient interface contract.
 * DalStoreFeignClient is a marker interface — it inherits IKvStore methods.
 * These tests verify the interface metadata and contract.
 */
class DalStoreFeignClientTest {

    @Test
    void interface_declared_as_FeignClient() {
        // Verify the interface has @FeignClient annotation
        FeignClient annotation = DalStoreFeignClient.class.getAnnotation(FeignClient.class);
        assertNotNull(annotation, "DalStoreFeignClient must have @FeignClient annotation");

        assertEquals("dal-service", annotation.name());
        assertEquals("/dal", annotation.path());
    }

    @Test
    void interface_extends_IKvStore() {
        // Verify it implements IKvStore contract
        assertTrue(io.mango.dal.api.IKvStore.class.isAssignableFrom(DalStoreFeignClient.class),
                "DalStoreFeignClient must extend IKvStore");
    }

    @Test
    void all_IKvStore_methods_areAvailable() {
        // Verify all IKvStore methods are accessible via DalStoreFeignClient
        // (either declared directly or inherited through interface extension)
        Method[] ikvStoreMethods = io.mango.dal.api.IKvStore.class.getDeclaredMethods();
        for (Method method : ikvStoreMethods) {
            if (method.getDeclaringClass() == io.mango.dal.api.IKvStore.class) {
                assertDoesNotThrow(
                    () -> DalStoreFeignClient.class.getMethod(method.getName(), method.getParameterTypes()),
                    "DalStoreFeignClient must expose IKvStore method: " + method.getName()
                );
            }
        }
    }
}
