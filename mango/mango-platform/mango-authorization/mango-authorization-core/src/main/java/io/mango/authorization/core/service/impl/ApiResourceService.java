package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.enums.AuthorizationCode;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.core.entity.ApiResourceEntity;
import io.mango.authorization.core.mapper.ApiResourceMapper;
import io.mango.authorization.core.service.IApiResourceService;
import io.mango.common.result.Require;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * API 资源服务实现。
 *
 * @author hardy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiResourceService implements IApiResourceService {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final int MAX_DECISION_CACHE_SIZE = 10_000;
    private static final int MAX_REGISTER_BATCH_SIZE = 10_000;

    private final ApiResourceMapper apiResourceMapper;

    private final Map<String, ApiResourceAccessDecisionVO> decisionCache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> decisionCacheKeys = new ConcurrentLinkedQueue<>();
    private volatile List<ApiResourceEntity> activeResourceCache;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResourceRegisterResultVO registerApiResources(List<ApiResourceRegisterCommand> resources) {
        Require.isTrue(resources == null || resources.size() <= MAX_REGISTER_BATCH_SIZE,
                AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "API 资源批量注册数量不能超过10000条");
        return registerApiResources(resources, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResourceRegisterResultVO upsertApiResources(List<ApiResourceRegisterCommand> resources) {
        Require.isTrue(resources == null || resources.size() <= MAX_REGISTER_BATCH_SIZE,
                AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "API 资源批量注册数量不能超过10000条");
        return registerApiResources(resources, false);
    }

    private ApiResourceRegisterResultVO registerApiResources(
            List<ApiResourceRegisterCommand> resources, boolean disableStaleResources) {
        if (resources == null || resources.isEmpty()) {
            return ApiResourceRegisterResultVO.empty();
        }

        List<ApiResourceRegisterCommand> validResources = resources.stream()
                .filter(resource -> {
                    boolean valid = isValid(resource);
                    if (!valid) {
                        log.warn("Skip invalid API resource: {}", resource);
                    }
                    return valid;
                })
                .toList();
        if (validResources.isEmpty()) {
            return new ApiResourceRegisterResultVO(0, 0, 0);
        }
        validResources = deduplicate(validResources);

        Set<String> currentResourceKeys = validResources.stream()
                .map(resource -> resourceKey(
                        resource.getModuleName(),
                        resource.getHttpMethod(),
                        resource.getPathPattern()))
                .collect(Collectors.toSet());
        Set<String> currentRouteKeys = validResources.stream()
                .map(resource -> routeKey(resource.getHttpMethod(), resource.getPathPattern()))
                .collect(Collectors.toSet());
        Map<String, ApiResourceEntity> existingIndex = loadExistingIndex(validResources);
        List<ApiResourceEntity> creates = new ArrayList<>();
        List<ApiResourceEntity> updates = new ArrayList<>();
        for (ApiResourceRegisterCommand resource : validResources) {
            ApiResourceEntity existing = existingIndex.get(resourceKey(
                    resource.getModuleName(),
                    resource.getHttpMethod(),
                    resource.getPathPattern()));
            if (existing == null) {
                creates.add(toEntity(resource));
            } else {
                merge(existing, resource);
                updates.add(existing);
            }
        }
        List<ApiResourceEntity> staleResources = disableStaleResources
                ? loadStaleAutoScannedResources(validResources, currentResourceKeys) : List.of();
        List<ApiResourceEntity> duplicateRouteResources = loadDuplicateRouteResources(validResources, currentResourceKeys, currentRouteKeys);
        staleResources.forEach(resource -> resource.setStatus(0));
        duplicateRouteResources.forEach(resource -> resource.setStatus(0));
        updates.addAll(staleResources);
        updates.addAll(duplicateRouteResources);
        updates = deduplicateEntities(updates);
        if (!creates.isEmpty()) {
            creates.forEach(apiResourceMapper::insert);
        }
        if (!updates.isEmpty()) {
            updates.forEach(apiResourceMapper::updateById);
        }
        if (!staleResources.isEmpty()) {
            log.info("API resource stale entries disabled: count={}", staleResources.size());
        }
        if (!duplicateRouteResources.isEmpty()) {
            log.info("API resource duplicate route entries disabled: count={}", duplicateRouteResources.size());
        }
        clearRuntimeCache();
        return new ApiResourceRegisterResultVO(validResources.size(), creates.size(), updates.size());
    }

    @Override
    public ApiResourceAccessDecisionVO resolveAccessDecision(String httpMethod, String path) {
        if (!StringUtils.hasText(httpMethod) || !StringUtils.hasText(path)) {
            return ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN);
        }
        String method = httpMethod.toUpperCase();
        String cacheKey = method + "\n" + path;
        ApiResourceAccessDecisionVO cached = decisionCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        ApiResourceAccessDecisionVO decision = activeResources().stream()
                .filter(resource -> methodMatches(resource.getHttpMethod(), method))
                .sorted(resourceMatchComparator(path))
                .filter(resource -> pathMatches(resource.getPathPattern(), path))
                .findFirst()
                .map(this::toDecision)
                .orElseGet(() -> ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN));
        cacheDecision(cacheKey, decision);
        return decision;
    }

    @Override
    public void refreshRuntimeCache() {
        clearRuntimeCache();
    }

    private Map<String, ApiResourceEntity> loadExistingIndex(List<ApiResourceRegisterCommand> resources) {
        List<String> moduleNames = resources.stream()
                .map(ApiResourceRegisterCommand::getModuleName)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (moduleNames.isEmpty()) {
            return Map.of();
        }
        List<ApiResourceEntity> existingResources = apiResourceMapper.selectList(new LambdaQueryWrapper<ApiResourceEntity>()
                .in(ApiResourceEntity::getModuleName, moduleNames));
        Map<String, ApiResourceEntity> index = new HashMap<>(existingResources.size());
        existingResources.forEach(resource -> index.put(resourceKey(
                resource.getModuleName(),
                resource.getHttpMethod(),
                resource.getPathPattern()), resource));
        return index;
    }

    private List<ApiResourceEntity> loadStaleAutoScannedResources(
            List<ApiResourceRegisterCommand> resources,
            Set<String> currentResourceKeys) {
        List<String> handlerClasses = resources.stream()
                .map(ApiResourceRegisterCommand::getHandlerClass)
                .filter(StringUtils::hasText)
                .filter(handlerClass -> !"configuration".equals(handlerClass))
                .distinct()
                .toList();
        if (handlerClasses.isEmpty()) {
            return List.of();
        }
        return apiResourceMapper.selectList(new LambdaQueryWrapper<ApiResourceEntity>()
                .in(ApiResourceEntity::getHandlerClass, handlerClasses)
                .eq(ApiResourceEntity::getStatus, 1))
                .stream()
                .filter(resource -> !currentResourceKeys.contains(resourceKey(
                        resource.getModuleName(),
                        resource.getHttpMethod(),
                        resource.getPathPattern())))
                .collect(Collectors.toList());
    }

    private List<ApiResourceEntity> loadDuplicateRouteResources(
            List<ApiResourceRegisterCommand> resources,
            Set<String> currentResourceKeys,
            Set<String> currentRouteKeys) {
        List<String> pathPatterns = resources.stream()
                .map(ApiResourceRegisterCommand::getPathPattern)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (pathPatterns.isEmpty()) {
            return List.of();
        }
        return apiResourceMapper.selectList(new LambdaQueryWrapper<ApiResourceEntity>()
                .in(ApiResourceEntity::getPathPattern, pathPatterns)
                .eq(ApiResourceEntity::getStatus, 1))
                .stream()
                .filter(resource -> currentRouteKeys.contains(routeKey(resource.getHttpMethod(), resource.getPathPattern())))
                .filter(resource -> !currentResourceKeys.contains(resourceKey(
                        resource.getModuleName(),
                        resource.getHttpMethod(),
                        resource.getPathPattern())))
                .collect(Collectors.toList());
    }

    private List<ApiResourceRegisterCommand> deduplicate(List<ApiResourceRegisterCommand> resources) {
        Map<String, ApiResourceRegisterCommand> index = new LinkedHashMap<>();
        resources.forEach(resource -> index.put(resourceKey(
                resource.getModuleName(),
                resource.getHttpMethod(),
                resource.getPathPattern()), resource));
        return new ArrayList<>(index.values());
    }

    private List<ApiResourceEntity> deduplicateEntities(List<ApiResourceEntity> resources) {
        Map<Long, ApiResourceEntity> index = new LinkedHashMap<>();
        resources.forEach(resource -> index.put(resource.getId(), resource));
        return new ArrayList<>(index.values());
    }

    private List<ApiResourceEntity> activeResources() {
        List<ApiResourceEntity> cached = activeResourceCache;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (activeResourceCache == null) {
                activeResourceCache = apiResourceMapper.selectList(new LambdaQueryWrapper<ApiResourceEntity>()
                        .eq(ApiResourceEntity::getStatus, 1))
                        .stream()
                        .filter(resource -> StringUtils.hasText(resource.getPathPattern()))
                        .collect(Collectors.toList());
            }
            return activeResourceCache;
        }
    }

    private void clearRuntimeCache() {
        activeResourceCache = null;
        decisionCache.clear();
        decisionCacheKeys.clear();
    }

    private void cacheDecision(String cacheKey, ApiResourceAccessDecisionVO decision) {
        ApiResourceAccessDecisionVO existing = decisionCache.putIfAbsent(cacheKey, decision);
        if (existing != null) {
            return;
        }
        decisionCacheKeys.add(cacheKey);
        while (decisionCache.size() > MAX_DECISION_CACHE_SIZE) {
            String eldest = decisionCacheKeys.poll();
            if (eldest == null) {
                return;
            }
            decisionCache.remove(eldest);
        }
    }

    private boolean methodMatches(String registeredMethod, String requestMethod) {
        return "ALL".equalsIgnoreCase(registeredMethod)
                || requestMethod.equalsIgnoreCase(registeredMethod);
    }

    private String resourceKey(String moduleName, String httpMethod, String pathPattern) {
        return moduleName + "\n" + httpMethod + "\n" + pathPattern;
    }

    private String routeKey(String httpMethod, String pathPattern) {
        return httpMethod + "\n" + pathPattern;
    }

    private boolean isValid(ApiResourceRegisterCommand resource) {
        return resource != null
                && StringUtils.hasText(resource.getModuleName())
                && StringUtils.hasText(resource.getHttpMethod())
                && StringUtils.hasText(resource.getPathPattern());
    }

    private ApiResourceEntity toEntity(ApiResourceRegisterCommand resource) {
        ApiResourceEntity entity = new ApiResourceEntity();
        entity.setTenantId("default");
        entity.setStatus(1);
        entity.setDeleted(0);
        merge(entity, resource);
        return entity;
    }

    private void merge(ApiResourceEntity entity, ApiResourceRegisterCommand resource) {
        entity.setModuleName(resource.getModuleName());
        entity.setHttpMethod(resource.getHttpMethod());
        entity.setPathPattern(resource.getPathPattern());
        entity.setResourceCode(defaultResourceCode(resource));
        entity.setPermissionCode(resource.getPermissionCode());
        entity.setAccessMode(defaultAccessMode(resource).name());
        entity.setHandlerClass(resource.getHandlerClass());
        entity.setHandlerMethod(resource.getHandlerMethod());
        entity.setDescription(resource.getDescription());
        entity.setStatus(1);
    }

    private String defaultResourceCode(ApiResourceRegisterCommand resource) {
        if (StringUtils.hasText(resource.getResourceCode())) {
            return resource.getResourceCode();
        }
        return resource.getHttpMethod() + ":" + resource.getPathPattern();
    }

    private ApiResourceAccessMode defaultAccessMode(ApiResourceRegisterCommand resource) {
        return resource.getAccessMode() == null ? ApiResourceAccessMode.LOGIN : resource.getAccessMode();
    }

    private ApiResourceAccessDecisionVO toDecision(ApiResourceEntity resource) {
        ApiResourceAccessMode mode = parseAccessMode(resource.getAccessMode());
        return new ApiResourceAccessDecisionVO(true, mode, resource.getPermissionCode());
    }

    private ApiResourceAccessMode parseAccessMode(String value) {
        if (!StringUtils.hasText(value)) {
            return ApiResourceAccessMode.LOGIN;
        }
        try {
            return ApiResourceAccessMode.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown API resource access mode: {}", value);
            return ApiResourceAccessMode.LOGIN;
        }
    }

    private boolean pathMatches(String pattern, String path) {
        if (!StringUtils.hasText(pattern) || !StringUtils.hasText(path)) {
            return false;
        }
        if (pattern.equals(path)) {
            return true;
        }
        return PATH_MATCHER.match(pattern, path);
    }

    private Comparator<ApiResourceEntity> resourceMatchComparator(String path) {
        return Comparator
                .comparingInt((ApiResourceEntity resource) -> exactPathScore(resource.getPathPattern(), path))
                .reversed()
                .thenComparing(Comparator.comparingInt((ApiResourceEntity resource) -> wildcardScore(resource.getPathPattern()))
                        .reversed())
                .thenComparing(Comparator.comparingInt((ApiResourceEntity resource) -> resource.getPathPattern().length())
                        .reversed())
                .thenComparing(ApiResourceEntity::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private int exactPathScore(String pattern, String path) {
        return pattern != null && pattern.equals(path) ? 1 : 0;
    }

    private int wildcardScore(String pattern) {
        return pattern != null && !pattern.contains("*") && !pattern.contains("{") ? 1 : 0;
    }
}
