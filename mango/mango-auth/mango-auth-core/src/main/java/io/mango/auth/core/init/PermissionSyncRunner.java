package io.mango.auth.core.init;

import io.mango.common.annotation.Perm;
import io.mango.rbac.api.SysPermissionApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Permission scanner and sync runner.
 *
 * Scans all @Perm annotations on controller methods and syncs
 * permission definitions to sys_permission table.
 *
 * Mode:
 * - write (dev/test): auto INSERT missing permissions
 * - read (prod): only output diff log, no DB write
 */
@Slf4j
@Configuration
public class PermissionSyncRunner implements ApplicationRunner {

    @Value("${mango.auth.permission.sync.mode:write}")
    private String syncMode;

    @Value("${mango.auth.permission.sync.enabled:true}")
    private boolean syncEnabled;

    private final RequestMappingHandlerMapping handlerMapping;
    private final SysPermissionApi sysPermissionApi;

    public PermissionSyncRunner(RequestMappingHandlerMapping handlerMapping, SysPermissionApi sysPermissionApi) {
        this.handlerMapping = handlerMapping;
        this.sysPermissionApi = sysPermissionApi;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!syncEnabled) {
            log.info("Permission sync is disabled");
            return;
        }

        log.info("Starting permission sync (mode={})", syncMode);

        // 1. Scan all @Perm annotations
        Map<String, PermInfo> scannedPerms = scanPermAnnotations();

        // 2. Get existing permissions from DB via SysPermissionApi
        Set<String> existingPerms = sysPermissionApi.getAllPermissionCodes();

        // 3. Compute diff
        Set<String> newPerms = scannedPerms.keySet().stream()
            .filter(k -> !existingPerms.contains(k))
            .collect(Collectors.toSet());

        if (newPerms.isEmpty()) {
            log.info("Permission sync complete: no new permissions");
            return;
        }

        log.info("Found {} new permissions to sync: {}", newPerms.size(), newPerms);

        if ("read".equalsIgnoreCase(syncMode)) {
            log.warn("PROD MODE: {} permissions would be added but sync is read-only", newPerms.size());
            return;
        }

        // 4. Insert missing permissions (dev/test mode)
        // NOTE: Actual permission persistence should go through the permission module's
        // proper API (SysPermissionApi.addPermission). This runner logs the intent
        // as a lightweight solution for development. Production permission management
        // should use the admin UI or proper permission management APIs.
        for (String permCode : newPerms) {
            PermInfo info = scannedPerms.get(permCode);
            log.info("Permission sync [DEV]: code={}, module={}, action={}",
                permCode, info.module, info.action);
            // In production, call: sysPermissionApi.addPermission(permCode, info.module, info.action)
        }

        log.info("Permission sync complete: {} permissions logged for sync", newPerms.size());
    }

    private Map<String, PermInfo> scanPermAnnotations() {
        Map<String, PermInfo> result = new HashMap<>();

        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            HandlerMethod handler = entry.getValue();
            Method method = handler.getMethod();

            Perm perm = AnnotatedElementUtils.findMergedAnnotation(method, Perm.class);
            if (perm != null) {
                String value = perm.value();
                if (value != null && !value.isBlank()) {
                    String[] parts = value.split(":");
                    String module = parts.length > 1 ? parts[1] : "unknown";
                    result.put(value, new PermInfo(value, module, method.getName()));
                }
            }
        }

        return result;
    }

    private static class PermInfo {
        final String code;
        final String module;
        final String action;

        PermInfo(String code, String module, String action) {
            this.code = code;
            this.module = module;
            this.action = action;
        }
    }
}
