package io.mango.authorization.access.core.config;

import io.mango.authorization.api.PublicPathApi;
import io.mango.authorization.access.core.AccessConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 动态访问路径配置缓存。
 *
 * @author Mango
 */
@Slf4j
@RequiredArgsConstructor
public class DynamicWhiteListConfig {

    private final PublicPathApi publicPathApi;

    /**
     * 匿名访问路径。
     */
    private final CopyOnWriteArrayList<String> anonymousPaths = new CopyOnWriteArrayList<>();

    /**
     * 需要登录的路径。
     */
    private final CopyOnWriteArrayList<String> loginRequiredPaths = new CopyOnWriteArrayList<>();

    /**
     * 内部访问路径。
     */
    private final CopyOnWriteArrayList<String> internalPaths = new CopyOnWriteArrayList<>();

    /**
     * 应用启动后加载一次。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialLoad() {
        refreshWhiteList();
    }

    /**
     * 每 5 分钟刷新一次。
     */
    @Scheduled(fixedRate = 300_000)
    public void scheduledRefresh() {
        refreshWhiteList();
    }

    /**
     * 从授权服务刷新路径配置。
     */
    public void refreshWhiteList() {
        try {
            var anonymousResult = publicPathApi.getAnonymousPaths();
            var loginResult = publicPathApi.getLoginRequiredPaths();
            var internalResult = publicPathApi.listInternalPaths();

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

            log.info("刷新边界入口路径配置成功: anonymous={}, loginRequired={}, internal={}",
                    anonymousPaths.size(), loginRequiredPaths.size(), internalPaths.size());
        } catch (Exception e) {
            log.warn("刷新边界入口路径配置失败，使用默认匿名路径: {}", e.getMessage());
            loadDefaults();
        }
    }

    /**
     * 加载默认匿名路径。
     */
    private void loadDefaults() {
        if (anonymousPaths.isEmpty()) {
            anonymousPaths.addAll(Arrays.asList(AccessConstants.DEFAULT_ANONYMOUS_PATHS));
        }
    }

    /**
     * 判断是否匿名访问路径。
     */
    public boolean isAnonymousPath(String path) {
        return matchPath(anonymousPaths, path);
    }

    /**
     * 判断是否登录访问路径。
     */
    public boolean isLoginRequiredPath(String path) {
        return matchPath(loginRequiredPaths, path);
    }

    /**
     * 判断是否内部访问路径。
     */
    public boolean isInternalPath(String path) {
        return matchPath(internalPaths, path);
    }

    /**
     * 匹配路径列表。
     */
    private boolean matchPath(List<String> patterns, String path) {
        for (String pattern : patterns) {
            if (matchPattern(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchPattern(String pattern, String path) {
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
     * 查询匿名访问路径。
     */
    public List<String> getAnonymousPaths() {
        return List.copyOf(anonymousPaths);
    }

    /**
     * 查询登录访问路径。
     */
    public List<String> getLoginRequiredPaths() {
        return List.copyOf(loginRequiredPaths);
    }

    /**
     * 查询内部访问路径。
     */
    public List<String> getInternalPaths() {
        return List.copyOf(internalPaths);
    }
}
