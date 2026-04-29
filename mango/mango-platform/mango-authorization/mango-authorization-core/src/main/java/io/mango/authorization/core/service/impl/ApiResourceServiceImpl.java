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

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResourceRegisterResultVO registerApiResources(List<ApiResourceRegisterCommand> resources) {
        if (resources == null || resources.isEmpty()) {
            return ApiResourceRegisterResultVO.empty();
        }

        int created = 0;
        int updated = 0;
        for (ApiResourceRegisterCommand resource : resources) {
            if (!isValid(resource)) {
                log.warn("Skip invalid API resource: {}", resource);
                continue;
            }
            ApiResource existing = getOne(new LambdaQueryWrapper<ApiResource>()
                    .eq(ApiResource::getModuleName, resource.getModuleName())
                    .eq(ApiResource::getHttpMethod, resource.getHttpMethod())
                    .eq(ApiResource::getPathPattern, resource.getPathPattern())
                    .last("LIMIT 1"));
            if (existing == null) {
                save(toEntity(resource));
                created++;
                continue;
            }
            merge(existing, resource);
            updateById(existing);
            updated++;
        }
        return new ApiResourceRegisterResultVO(resources.size(), created, updated);
    }

    @Override
    public ApiResourceAccessDecisionVO resolveAccessDecision(String httpMethod, String path) {
        if (!StringUtils.hasText(httpMethod) || !StringUtils.hasText(path)) {
            return ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN);
        }
        List<ApiResource> resources = list(new LambdaQueryWrapper<ApiResource>()
                .eq(ApiResource::getStatus, 1)
                .and(wrapper -> wrapper
                        .eq(ApiResource::getHttpMethod, httpMethod)
                        .or()
                        .eq(ApiResource::getHttpMethod, "ALL")));
        return resources.stream()
                .sorted(Comparator.comparingInt((ApiResource resource) -> resource.getPathPattern().length()).reversed())
                .filter(resource -> pathMatches(resource.getPathPattern(), path))
                .findFirst()
                .map(this::toDecision)
                .orElseGet(() -> ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN));
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
