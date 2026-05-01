package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.core.entity.ApiResource;
import io.mango.authorization.core.mapper.ApiResourceMapper;
import io.mango.authorization.core.service.IApiResourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * API 资源服务实现。
 *
 * @author hardy
 */
@Slf4j
@Service
public class ApiResourceServiceImpl
        extends ServiceImpl<ApiResourceMapper, ApiResource>
        implements IApiResourceService {

    private final Map<String, ApiResourceAccessDecisionVO> decisionCache = new ConcurrentHashMap<>();
    private volatile List<ApiResource> activeResourceCache;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResourceRegisterResultVO registerApiResources(List<ApiResourceRegisterCommand> resources) {
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

        Map<String, ApiResource> existingIndex = loadExistingIndex(validResources);
        List<ApiResource> creates = new ArrayList<>();
        List<ApiResource> updates = new ArrayList<>();
        for (ApiResourceRegisterCommand resource : validResources) {
            ApiResource existing = existingIndex.get(resourceKey(
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
        if (!creates.isEmpty()) {
            saveBatch(creates);
        }
        if (!updates.isEmpty()) {
            updateBatchById(updates);
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
        return decisionCache.computeIfAbsent(cacheKey, ignored -> activeResources().stream()
                .filter(resource -> methodMatches(resource.getHttpMethod(), method))
                .sorted(Comparator.comparingInt((ApiResource resource) -> resource.getPathPattern().length()).reversed())
                .filter(resource -> pathMatches(resource.getPathPattern(), path))
                .findFirst()
                .map(this::toDecision)
                .orElseGet(() -> ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN)));
    }

    private Map<String, ApiResource> loadExistingIndex(List<ApiResourceRegisterCommand> resources) {
        List<String> moduleNames = resources.stream()
                .map(ApiResourceRegisterCommand::getModuleName)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (moduleNames.isEmpty()) {
            return Map.of();
        }
        List<ApiResource> existingResources = list(new LambdaQueryWrapper<ApiResource>()
                .in(ApiResource::getModuleName, moduleNames));
        Map<String, ApiResource> index = new HashMap<>(existingResources.size());
        existingResources.forEach(resource -> index.put(resourceKey(
                resource.getModuleName(),
                resource.getHttpMethod(),
                resource.getPathPattern()), resource));
        return index;
    }

    private List<ApiResourceRegisterCommand> deduplicate(List<ApiResourceRegisterCommand> resources) {
        Map<String, ApiResourceRegisterCommand> index = new LinkedHashMap<>();
        resources.forEach(resource -> index.put(resourceKey(
                resource.getModuleName(),
                resource.getHttpMethod(),
                resource.getPathPattern()), resource));
        return new ArrayList<>(index.values());
    }

    private List<ApiResource> activeResources() {
        List<ApiResource> cached = activeResourceCache;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (activeResourceCache == null) {
                activeResourceCache = list(new LambdaQueryWrapper<ApiResource>()
                        .eq(ApiResource::getStatus, 1))
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
    }

    private boolean methodMatches(String registeredMethod, String requestMethod) {
        return "ALL".equalsIgnoreCase(registeredMethod)
                || requestMethod.equalsIgnoreCase(registeredMethod);
    }

    private String resourceKey(String moduleName, String httpMethod, String pathPattern) {
        return moduleName + "\n" + httpMethod + "\n" + pathPattern;
    }

    private boolean isValid(ApiResourceRegisterCommand resource) {
        return resource != null
                && StringUtils.hasText(resource.getModuleName())
                && StringUtils.hasText(resource.getHttpMethod())
                && StringUtils.hasText(resource.getPathPattern());
    }

    private ApiResource toEntity(ApiResourceRegisterCommand resource) {
        ApiResource entity = new ApiResource();
        entity.setStatus(1);
        entity.setDeleted(0);
        merge(entity, resource);
        return entity;
    }

    private void merge(ApiResource entity, ApiResourceRegisterCommand resource) {
        entity.setModuleName(resource.getModuleName());
        entity.setHttpMethod(resource.getHttpMethod());
        entity.setPathPattern(resource.getPathPattern());
        entity.setResourceCode(defaultResourceCode(resource));
        entity.setPermissionCode(resource.getPermissionCode());
        entity.setAccessMode(defaultAccessMode(resource).name());
        entity.setHandlerClass(resource.getHandlerClass());
        entity.setHandlerMethod(resource.getHandlerMethod());
        entity.setDescription(resource.getDescription());
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
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

    private ApiResourceAccessDecisionVO toDecision(ApiResource resource) {
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
        return path.matches(toPathRegex(pattern));
    }

    private String toPathRegex(String pattern) {
        StringBuilder regex = new StringBuilder("^");
        for (int index = 0; index < pattern.length(); index++) {
            char current = pattern.charAt(index);
            if (current == '{') {
                int end = pattern.indexOf('}', index);
                if (end > index) {
                    regex.append("[^/]+");
                    index = end;
                    continue;
                }
            }
            if (current == '*') {
                if (index + 1 < pattern.length() && pattern.charAt(index + 1) == '*') {
                    regex.append(".*");
                    index++;
                } else {
                    regex.append("[^/]*");
                }
                continue;
            }
            regex.append(Pattern.quote(String.valueOf(current)));
        }
        regex.append("$");
        return regex.toString();
    }
}
