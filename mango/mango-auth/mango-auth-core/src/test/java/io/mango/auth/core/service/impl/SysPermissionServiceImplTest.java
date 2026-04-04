package io.mango.auth.core.service.impl;

import io.mango.permission.api.SysPermissionApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SysPermissionServiceImpl
 *
 * @author Mango
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysPermissionServiceImpl Tests")
class SysPermissionServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SysPermissionServiceImpl sysPermissionService;

    @BeforeEach
    void setUp() {
        sysPermissionService = new SysPermissionServiceImpl(jdbcTemplate);
    }

    @Test
    @DisplayName("getAllPermissionCodes should return permission codes from database")
    void getAllPermissionCodes_validData_returnsCodes() {
        Map<String, Object> row1 = new HashMap<>();
        row1.put("perm_code", "system:user:view");
        Map<String, Object> row2 = new HashMap<>();
        row2.put("perm_code", "system:user:add");
        when(jdbcTemplate.queryForList(anyString())).thenReturn(Arrays.asList(row1, row2));

        Set<String> result = sysPermissionService.getAllPermissionCodes();

        assertEquals(2, result.size());
        assertTrue(result.contains("system:user:view"));
        assertTrue(result.contains("system:user:add"));
    }

    @Test
    @DisplayName("getAllPermissionCodes should return empty set when no permissions")
    void getAllPermissionCodes_noData_returnsEmptySet() {
        when(jdbcTemplate.queryForList(anyString())).thenReturn(Arrays.asList());

        Set<String> result = sysPermissionService.getAllPermissionCodes();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getAllPermissionCodes should skip null or blank codes")
    void getAllPermissionCodes_withNullCode_skipsNull() {
        Map<String, Object> row1 = new HashMap<>();
        row1.put("perm_code", "system:user:view");
        Map<String, Object> row2 = new HashMap<>();
        row2.put("perm_code", null);
        Map<String, Object> row3 = new HashMap<>();
        row3.put("perm_code", "  ");
        when(jdbcTemplate.queryForList(anyString())).thenReturn(Arrays.asList(row1, row2, row3));

        Set<String> result = sysPermissionService.getAllPermissionCodes();

        assertEquals(1, result.size());
        assertTrue(result.contains("system:user:view"));
    }

    @Test
    @DisplayName("getAllPermissionCodes should return empty set on database error")
    void getAllPermissionCodes_databaseError_returnsEmptySet() {
        when(jdbcTemplate.queryForList(anyString())).thenThrow(new RuntimeException("Database error"));

        Set<String> result = sysPermissionService.getAllPermissionCodes();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("SysPermissionServiceImpl implements SysPermissionApi")
    void implementsSysPermissionApi() {
        assertTrue(sysPermissionService instanceof SysPermissionApi);
    }
}
