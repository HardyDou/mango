package io.mango.permission.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.mango.permission.core.entity.SysPublicPath;
import io.mango.permission.api.vo.SysPublicPathVO;
import io.mango.permission.core.mapper.SysPublicPathMapper;
import io.mango.permission.core.service.ISysPublicPathService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Public path service implementation
 *
 * @author Mango
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysPublicPathServiceImpl
        extends ServiceImpl<SysPublicPathMapper, SysPublicPath>
        implements ISysPublicPathService {

    /**
     * Cache of public paths (loaded from DB, refreshed periodically)
     */
    private volatile List<String> anonymousPathsCache = new ArrayList<>();
    private volatile List<String> loginRequiredPathsCache = new ArrayList<>();
    private volatile List<String> internalPathsCache = new ArrayList<>();
    private volatile long lastRefreshTime = 0;
    private static final long CACHE_TTL_MS = 60_000; // 1 minute cache

    @Override
    public List<SysPublicPathVO> listEnabled() {
        List<SysPublicPath> paths = list(
                new LambdaQueryWrapper<SysPublicPath>()
                        .eq(SysPublicPath::getStatus, 1)
                        .orderByDesc(SysPublicPath::getPriority)
                        .orderByAsc(SysPublicPath::getCreateTime)
        );
        return paths.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<String> getAnonymousPaths() {
        refreshCacheIfNeeded();
        return anonymousPathsCache;
    }

    @Override
    public List<String> getLoginRequiredPaths() {
        refreshCacheIfNeeded();
        return loginRequiredPathsCache;
    }

    @Override
    public List<String> listInternalPaths() {
        refreshCacheIfNeeded();
        return internalPathsCache;
    }

    @Override
    public boolean isPublicPath(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        refreshCacheIfNeeded();

        // Check anonymous paths (supports wildcard matching)
        for (String pattern : anonymousPathsCache) {
            if (matchPath(pattern, path)) {
                return true;
            }
        }

        // Check login-required paths
        for (String pattern : loginRequiredPathsCache) {
            if (matchPath(pattern, path)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean addPublicPath(SysPublicPath publicPath) {
        boolean result = save(publicPath);
        if (result) {
            invalidateCache();
        }
        return result;
    }

    @Override
    public boolean updatePublicPath(SysPublicPath publicPath) {
        boolean result = updateById(publicPath);
        if (result) {
            invalidateCache();
        }
        return result;
    }

    @Override
    public boolean deletePublicPath(Long id) {
        boolean result = removeById(id);
        if (result) {
            invalidateCache();
        }
        return result;
    }

    /**
     * Match path against pattern (supports ** wildcard)
     */
    private boolean matchPath(String pattern, String path) {
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
     * Refresh cache if expired
     */
    private void refreshCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime > CACHE_TTL_MS) {
            synchronized (this) {
                if (now - lastRefreshTime > CACHE_TTL_MS) {
                    refreshCache();
                }
            }
        }
    }

    /**
     * Refresh cache from database
     */
    private void refreshCache() {
        try {
            List<SysPublicPath> paths = list(
                    new LambdaQueryWrapper<SysPublicPath>()
                            .eq(SysPublicPath::getStatus, 1)
            );

            Map<Integer, List<String>> pathsByType = paths.stream()
                    .collect(Collectors.groupingBy(
                            SysPublicPath::getPathType,
                            Collectors.mapping(SysPublicPath::getPath, Collectors.toList())
                    ));

            anonymousPathsCache = pathsByType.getOrDefault(SysPublicPath.TYPE_ANONYMOUS, new ArrayList<>());
            loginRequiredPathsCache = pathsByType.getOrDefault(SysPublicPath.TYPE_LOGIN, new ArrayList<>());
            internalPathsCache = pathsByType.getOrDefault(SysPublicPath.TYPE_INTERNAL, new ArrayList<>());
            lastRefreshTime = System.currentTimeMillis();

            log.debug("Refreshed public path cache: anonymous={}, login-required={}, internal={}",
                    anonymousPathsCache.size(), loginRequiredPathsCache.size(), internalPathsCache.size());
        } catch (Exception e) {
            log.error("Failed to refresh public path cache", e);
        }
    }

    /**
     * Invalidate cache (called after CRUD operations)
     */
    private void invalidateCache() {
        lastRefreshTime = 0; // Force refresh on next access
    }

    /**
     * Convert entity to VO
     */
    private SysPublicPathVO toVO(SysPublicPath entity) {
        SysPublicPathVO vo = new SysPublicPathVO();
        vo.setId(entity.getId());
        vo.setPath(entity.getPath());
        vo.setPathType(entity.getPathType());
        vo.setPathTypeName(getTypeName(entity.getPathType()));
        vo.setDescription(entity.getDescription());
        vo.setPriority(entity.getPriority());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private String getTypeName(Integer type) {
        if (type == null) return "未知";
        if (type == SysPublicPath.TYPE_ANONYMOUS) return "匿名访问";
        if (type == SysPublicPath.TYPE_LOGIN) return "登录访问";
        if (type == SysPublicPath.TYPE_PERMISSION) return "权限访问";
        if (type == SysPublicPath.TYPE_INTERNAL) return "内部专用";
        return "未知";
    }
}
