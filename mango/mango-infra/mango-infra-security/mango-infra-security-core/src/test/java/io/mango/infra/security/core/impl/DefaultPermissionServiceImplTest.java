package io.mango.infra.security.core.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DefaultPermissionServiceImpl
 *
 * @author Mango
 */
@DisplayName("DefaultPermissionServiceImpl Tests")
class DefaultPermissionServiceImplTest {

    private DefaultPermissionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DefaultPermissionServiceImpl();
    }

    @AfterEach
    void tearDown() {
        service.clear();
    }

    @Test
    @DisplayName("listUserPermissions with no data should return empty list")
    void listUserPermissions_noData_returnsEmptyList() {
        List<String> result = service.listUserPermissions(1L);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("listUserPermissions with null userId should return empty list")
    void listUserPermissions_nullUserId_returnsEmptyList() {
        List<String> result = service.listUserPermissions(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("listUserPermissions with existing user should return permissions")
    void listUserPermissions_existingUser_returnsPermissions() {
        service.addPermissions(1L, List.of("user:test:add", "user:test:view"));

        List<String> result = service.listUserPermissions(1L);

        assertEquals(2, result.size());
        assertTrue(result.contains("user:test:add"));
        assertTrue(result.contains("user:test:view"));
    }

    @Test
    @DisplayName("listUserPermissions for different user should return their own permissions")
    void listUserPermissions_differentUser_returnsOwnPermissions() {
        service.addPermissions(1L, List.of("user:user1:add"));
        service.addPermissions(2L, List.of("user:user2:add"));

        List<String> result = service.listUserPermissions(2L);

        assertEquals(1, result.size());
        assertTrue(result.contains("user:user2:add"));
        assertFalse(result.contains("user:user1:add"));
    }

    @Test
    @DisplayName("clear should remove all permissions")
    void clear_withData_removesAll() {
        service.addPermissions(1L, List.of("user:test:add"));
        service.addPermissions(2L, List.of("user:test:view"));

        service.clear();

        assertTrue(service.listUserPermissions(1L).isEmpty());
        assertTrue(service.listUserPermissions(2L).isEmpty());
    }

    @Test
    @DisplayName("addPermissions with null values should not throw")
    void addPermissions_nullValues_noException() {
        assertDoesNotThrow(() -> {
            service.addPermissions(null, null);
            service.addPermissions(1L, null);
            service.addPermissions(null, List.of("perm"));
        });
    }
}
