package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.mango.authorization.core.entity.PublicPath;
import io.mango.authorization.api.vo.PublicPathVO;
import io.mango.authorization.core.mapper.PublicPathMapper;
import io.mango.authorization.core.service.IPublicPathService;
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
public class PublicPathServiceImpl
        extends ServiceImpl<PublicPathMapper, PublicPath>
        implements IPublicPathService {

    /**
     * Cache of public paths (loaded from DB, refreshed periodically)
     */
    private volatile List<String> anonymousPathsCache = new ArrayList<>();
    private volatile List<String> loginRequiredPathsCache = new ArrayList<>();
    private volatile List<String> internalPathsCache = new ArrayList<>();
    private volatile long lastRefreshTime = 0;
    private static final long CACHE_TTL_MS = 60_000; // 1 minute cache

    @Override
    public List<PublicPathVO> listEnabled() {
        List<PublicPath> paths = list(
                new LambdaQueryWrapper<PublicPath>()
                        .eq(PublicPath::getStatus, 1)
                        .orderByDesc(PublicPath::getPriority)
                        .orderByAsc(PublicPath::getCreateTime)
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

        // 检查匿名访问路径，支持通配符匹配。
        for (String pattern : anonymousPathsCache) {
            if (matchPath(pattern, path)) {
                return true;
            }
        }

        // 检查需要登录的路径。
        for (String pattern : loginRequiredPathsCache) {
            if (matchPath(pattern, path)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean addPublicPath(PublicPath publicPath) {
        boolean result = save(publicPath);
        if (result) {
            invalidateCache();
        }
        return result;
    }

    @Override
    public boolean updatePublicPath(PublicPath publicPath) {
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
     * 根据路径模式匹配请求路径，支持 ** 通配符。
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
     * 缓存过期时刷新缓存。
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
     * 从数据库刷新缓存。
     */
    private void refreshCache() {
        try {
            List<PublicPath> paths = list(
                    new LambdaQueryWrapper<PublicPath>()
                            .eq(PublicPath::getStatus, 1)
            );

            Map<Integer, List<String>> pathsByType = paths.stream()
                    .collect(Collectors.groupingBy(
                            PublicPath::getPathType,
                            Collectors.mapping(PublicPath::getPath, Collectors.toList())
                    ));

            anonymousPathsCache = pathsByType.getOrDefault(PublicPath.TYPE_ANONYMOUS, new ArrayList<>());
            loginRequiredPathsCache = pathsByType.getOrDefault(PublicPath.TYPE_LOGIN, new ArrayList<>());
            internalPathsCache = pathsByType.getOrDefault(PublicPath.TYPE_INTERNAL, new ArrayList<>());
            lastRefreshTime = System.currentTimeMillis();

            log.debug("Refreshed public path cache: anonymous={}, login-required={}, internal={}",
                    anonymousPathsCache.size(), loginRequiredPathsCache.size(), internalPathsCache.size());
        } catch (Exception e) {
            log.error("Failed to refresh public path cache", e);
        }
    }

    /**
     * 使缓存失效，供增删改操作后调用。
     */
    private void invalidateCache() {
        lastRefreshTime = 0; // 下次访问时强制刷新。
    }

    /**
     * 转换实体为视图对象。
     */
    private PublicPathVO toVO(PublicPath entity) {
        PublicPathVO vo = new PublicPathVO();
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
        if (type == PublicPath.TYPE_ANONYMOUS) return "匿名访问";
        if (type == PublicPath.TYPE_LOGIN) return "登录访问";
        if (type == PublicPath.TYPE_PERMISSION) return "权限访问";
        if (type == PublicPath.TYPE_INTERNAL) return "内部专用";
        return "未知";
    }
}
