package io.mango.authorization.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.core.entity.ApiResourceEntity;
import io.mango.authorization.core.mapper.ApiResourceMapper;
import io.mango.authorization.core.service.IApiResourceService;
import io.mango.authorization.core.support.AuthorizationResourceIds;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.api.enums.ResourceSyncMode;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.support.model.ResourceSyncContext;
import io.mango.resource.support.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * mango-resource API_RESOURCE 目标处理器。
 */
@Component
@RequiredArgsConstructor
public class ApiResourceHandler implements ResourceHandler {

    private final IApiResourceService apiResourceService;
    private final ApiResourceMapper apiResourceMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.API_RESOURCE;
    }

    @Override
    public boolean requiresCompleteBatch() {
        return true;
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        return upsertBatch(List.of(resource)).get(resource.getId());
    }

    @Override
    public Map<String, ResourceSyncResult> upsertBatch(List<ResourceDeclaration> resources) {
        return synchronize(resources, false);
    }

    @Override
    public Map<String, ResourceSyncResult> upsertBatchWithContext(
            List<ResourceDeclaration> declarations,
            List<ResourceDeclaration> completeBatch,
            Map<String, ResourceSyncContext> syncContexts) {
        return synchronize(declarations, true);
    }

    private Map<String, ResourceSyncResult> synchronize(
            List<ResourceDeclaration> resources, boolean incremental) {
        List<ApiResourceRegisterCommand> commands = resources.stream()
                .map(this::toCommand)
                .toList();
        Map<String, ApiResourceEntity> protectedResources = protectedResources(resources, commands);
        if (incremental) {
            apiResourceService.upsertApiResources(commands);
        } else {
            apiResourceService.registerApiResources(commands);
        }
        restoreProtectedResources(protectedResources);
        Map<ApiResourceKey, ApiResourceEntity> synchronizedResources = loadResourceIndex(commands);
        Map<String, ResourceSyncResult> results = new LinkedHashMap<>();
        for (int index = 0; index < resources.size(); index++) {
            ResourceDeclaration resource = resources.get(index);
            ApiResourceEntity entity = synchronizedResources.get(ApiResourceKey.from(commands.get(index)));
            Long targetId = entity == null ? null : entity.getId();
            results.put(resource.getId(),
                    ResourceSyncResult.of(targetId, "authorization_api_resource", "api resource synced"));
        }
        return results;
    }

    private Map<String, ApiResourceEntity> protectedResources(
            List<ResourceDeclaration> resources,
            List<ApiResourceRegisterCommand> commands) {
        boolean requiresProtection = resources.stream()
                .anyMatch(resource -> resource.getSyncMode() != ResourceSyncMode.AUTO);
        if (!requiresProtection) {
            return Map.of();
        }
        Map<ApiResourceKey, ApiResourceEntity> existingResources = loadResourceIndex(commands);
        Map<String, ApiResourceEntity> protectedResources = new LinkedHashMap<>();
        for (int index = 0; index < resources.size(); index++) {
            ResourceDeclaration resource = resources.get(index);
            if (resource.getSyncMode() == ResourceSyncMode.AUTO) {
                continue;
            }
            ApiResourceEntity entity = existingResources.get(ApiResourceKey.from(commands.get(index)));
            if (entity != null) {
                protectedResources.put(resource.getId(), entity);
            }
        }
        return protectedResources;
    }

    private void restoreProtectedResources(Map<String, ApiResourceEntity> protectedResources) {
        for (ApiResourceEntity entity : protectedResources.values()) {
            apiResourceMapper.updateById(entity);
        }
    }

    private Map<ApiResourceKey, ApiResourceEntity> loadResourceIndex(List<ApiResourceRegisterCommand> commands) {
        List<String> moduleNames = commands.stream()
                .map(ApiResourceRegisterCommand::getModuleName)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (moduleNames.isEmpty()) {
            return Map.of();
        }
        Map<ApiResourceKey, ApiResourceEntity> resources = new LinkedHashMap<>();
        apiResourceMapper.selectList(new LambdaQueryWrapper<ApiResourceEntity>()
                        .in(ApiResourceEntity::getModuleName, moduleNames))
                .forEach(resource -> resources.put(ApiResourceKey.from(resource), resource));
        return resources;
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        Long targetId = longField(resource, "targetId");
        ApiResourceEntity entity = targetId == null ? find(toCommand(resource)) : apiResourceMapper.selectById(targetId);
        if (entity != null) {
            entity.setStatus(0);
            apiResourceMapper.updateById(entity);
            return ResourceSyncResult.of(entity.getId(), "authorization_api_resource", "api resource disabled");
        }
        return ResourceSyncResult.of(targetId, "authorization_api_resource", "api resource already missing");
    }

    private ApiResourceRegisterCommand toCommand(ResourceDeclaration resource) {
        ApiResourceRegisterCommand command = new ApiResourceRegisterCommand();
        command.setModuleName(requiredString(resource, "moduleName"));
        command.setHttpMethod(requiredString(resource, "httpMethod"));
        command.setPathPattern(requiredString(resource, "pathPattern"));
        command.setResourceId(AuthorizationResourceIds.declaredOrStable(
                longField(resource, "targetId"), "authorization_api_resource",
                command.getModuleName(), command.getHttpMethod(), command.getPathPattern()));
        command.setResourceCode(stringField(resource, "resourceCode"));
        command.setPermissionCode(stringField(resource, "permissionCode"));
        String accessMode = stringField(resource, "accessMode");
        if (StringUtils.hasText(accessMode)) {
            command.setAccessMode(ApiResourceAccessMode.valueOf(accessMode));
        }
        command.setHandlerClass(stringField(resource, "handlerClass"));
        command.setHandlerMethod(stringField(resource, "handlerMethod"));
        command.setDescription(stringField(resource, "description"));
        return command;
    }

    private ApiResourceEntity find(ApiResourceRegisterCommand command) {
        return apiResourceMapper.selectOne(new LambdaQueryWrapper<ApiResourceEntity>()
                .eq(ApiResourceEntity::getModuleName, command.getModuleName())
                .eq(ApiResourceEntity::getHttpMethod, command.getHttpMethod())
                .eq(ApiResourceEntity::getPathPattern, command.getPathPattern())
                .last("limit 1"));
    }

    private String requiredString(ResourceDeclaration resource, String fieldName) {
        String value = stringField(resource, fieldName);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("API resource field is required: " + fieldName);
        }
        return value;
    }

    private String stringField(ResourceDeclaration resource, String fieldName) {
        ResourceField field = resource.getFields().get(fieldName);
        return field == null || field.getValue() == null ? null : String.valueOf(field.getValue());
    }

    private Long longField(ResourceDeclaration resource, String fieldName) {
        String value = stringField(resource, fieldName);
        return StringUtils.hasText(value) ? Long.valueOf(value) : null;
    }

    private record ApiResourceKey(String moduleName, String httpMethod, String pathPattern) {

        private static ApiResourceKey from(ApiResourceRegisterCommand command) {
            return new ApiResourceKey(command.getModuleName(), command.getHttpMethod(), command.getPathPattern());
        }

        private static ApiResourceKey from(ApiResourceEntity entity) {
            return new ApiResourceKey(entity.getModuleName(), entity.getHttpMethod(), entity.getPathPattern());
        }
    }
}
