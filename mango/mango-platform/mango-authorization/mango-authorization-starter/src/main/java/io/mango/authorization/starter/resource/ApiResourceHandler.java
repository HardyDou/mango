package io.mango.authorization.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.core.entity.ApiResourceEntity;
import io.mango.authorization.core.mapper.ApiResourceMapper;
import io.mango.authorization.core.service.IApiResourceService;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.api.enums.ResourceSyncMode;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
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
        Map<String, ApiResourceEntity> protectedResources = protectedResources(resources);
        List<ApiResourceRegisterCommand> commands = resources.stream()
                .map(this::toCommand)
                .toList();
        apiResourceService.registerApiResources(commands);
        restoreProtectedResources(protectedResources);
        Map<String, ResourceSyncResult> results = new LinkedHashMap<>();
        for (ResourceDeclaration resource : resources) {
            ApiResourceEntity entity = find(toCommand(resource));
            Long targetId = entity == null ? null : entity.getId();
            results.put(resource.getId(),
                    ResourceSyncResult.of(targetId, "authorization_api_resource", "api resource synced"));
        }
        return results;
    }

    private Map<String, ApiResourceEntity> protectedResources(List<ResourceDeclaration> resources) {
        Map<String, ApiResourceEntity> protectedResources = new LinkedHashMap<>();
        for (ResourceDeclaration resource : resources) {
            if (resource.getSyncMode() == ResourceSyncMode.AUTO) {
                continue;
            }
            ApiResourceEntity entity = find(toCommand(resource));
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
}
