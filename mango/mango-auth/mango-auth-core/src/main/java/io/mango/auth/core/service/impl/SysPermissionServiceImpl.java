package io.mango.auth.core.service.impl;

import io.mango.permission.api.SysPermissionApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * System permission service implementation.
 * Queries sys_permission table for permission codes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysPermissionServiceImpl implements SysPermissionApi {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Set<String> getAllPermissionCodes() {
        try {
            // Query all permission codes from sys_permission table
            // This assumes the sys_permission table exists and has perm_code column
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT perm_code FROM sys_permission WHERE del_flag = 0 AND status = 1"
            );
            Set<String> codes = new HashSet<>();
            for (Map<String, Object> row : results) {
                String code = (String) row.get("perm_code");
                if (code != null && !code.isBlank()) {
                    codes.add(code);
                }
            }
            log.debug("Loaded {} permission codes from database", codes.size());
            return codes;
        } catch (Exception e) {
            // CRITICAL: Database unavailable - users will have no permissions
            // This is a degraded mode to allow application startup, but users cannot authenticate
            // properly until the database is available
            log.error("CRITICAL: Failed to query permission codes from database: {}. " +
                "Users will have no permissions until this is resolved. " +
                "Check database connectivity and sys_permission table existence.", e.getMessage());
            return Set.of(); // Return empty set to allow startup (degraded mode)
        }
    }
}
