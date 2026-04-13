package io.mango.gateway.core.config;

import io.mango.gateway.api.GatewayConstant;
import io.mango.gateway.api.SysPublicPathApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic white list loader
 * Loads public paths from database and caches them
 *
 * @author Mango
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class DynamicWhiteListConfig {

    private final SysPublicPathApi sysPublicPathApi;

    /**
     * Cached anonymous paths (loaded from DB)
     */
    private final CopyOnWriteArrayList<String> anonymousPaths = new CopyOnWriteArrayList<>();

    /**
     * Cached login-required paths
     */
    private final CopyOnWriteArrayList<String> loginRequiredPaths = new CopyOnWriteArrayList<>();

    /**
     * Cached internal-only paths (type=4)
     */
    private final CopyOnWriteArrayList<String> internalPaths = new CopyOnWriteArrayList<>();

    /**
     * Last refresh timestamp
     */
    private volatile long lastRefreshTime = 0;

    /**
     * Cache TTL in milliseconds (5 minutes)
     */
    private static final long CACHE_TTL_MS = 300_000;

    /**
     * Initial load on application ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialLoad() {
        refreshWhiteList();
    }

    /**
     * Periodic refresh every 5 minutes
     */
    @Scheduled(fixedRate = 300_000)
    public void scheduledRefresh() {
        refreshWhiteList();
    }

    /**
     * Refresh white list from database
     */
    public void refreshWhiteList() {
        try {
            // Try to load from database via Feign
            var anonymousResult = sysPublicPathApi.getAnonymousPaths();
            var loginResult = sysPublicPathApi.getLoginRequiredPaths();
            var internalResult = sysPublicPathApi.listInternalPaths();

            if (anonymousResult != null && anonymousResult.getData() != null) {
                anonymousPaths.clear();
                anonymousPaths.addAll(anonymousResult.getData());
            }

            if (loginResult != null && loginResult.getData() != null) {
                loginRequiredPaths.clear();
                loginRequiredPaths.addAll(loginResult.getData());
            }

            if (internalResult != null && internalResult.getData() != null) {
                internalPaths.clear();
                internalPaths.addAll(internalResult.getData());
            }

            lastRefreshTime = System.currentTimeMillis();
            log.info("Refreshed dynamic white list: anonymous={}, loginRequired={}, internal={}",
                    anonymousPaths.size(), loginRequiredPaths.size(), internalPaths.size());
        } catch (Exception e) {
            log.warn("Failed to refresh dynamic white list from DB, using defaults: {}", e.getMessage());
            // Load defaults if DB fetch fails
            loadDefaults();
        }
    }

    /**
     * Load default white list from GatewayConstant
     */
    private void loadDefaults() {
        if (anonymousPaths.isEmpty()) {
            anonymousPaths.addAll(Arrays.asList(GatewayConstant.WHITE_LIST));
        }
    }

    /**
     * Check if path is in anonymous white list
     */
    public boolean isAnonymousPath(String path) {
        return matchPath(anonymousPaths, path);
    }

    /**
     * Check if path is in login-required white list
     */
    public boolean isLoginRequiredPath(String path) {
        return matchPath(loginRequiredPaths, path);
    }

    /**
     * Check if path is public (anonymous or login-required)
     */
    public boolean isPublicPath(String path) {
        return isAnonymousPath(path) || isLoginRequiredPath(path);
    }

    /**
     * Check if path is internal-only (type=4)
     */
    public boolean isInternalPath(String path) {
        return matchPath(internalPaths, path);
    }

    /**
     * Match path against patterns (supports ** and *)
     */
    private boolean matchPath(List<String> patterns, String path) {
        for (String pattern : patterns) {
            if (matchPattern(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Match path against a single pattern
     */
    private boolean matchPattern(String pattern, String path) {
        if (pattern.equals(path)) {
            return true;
        }
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix) || path.equals(prefix.substring(0, prefix.length() - 1));
        }
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            int slashIndex = path.indexOf('/', prefix.length());
            return path.startsWith(prefix) && (slashIndex == -1 || slashIndex == path.length() - 1);
        }
        return false;
    }

    /**
     * Get all anonymous paths
     */
    public List<String> getAnonymousPaths() {
        return List.copyOf(anonymousPaths);
    }

    /**
     * Get all login-required paths
     */
    public List<String> getLoginRequiredPaths() {
        return List.copyOf(loginRequiredPaths);
    }

    /**
     * Get all internal paths
     */
    public List<String> getInternalPaths() {
        return List.copyOf(internalPaths);
    }
}
