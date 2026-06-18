package io.mango.authorization.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.core.entity.ApiResource;
import io.mango.authorization.core.mapper.ApiResourceMapper;
import io.mango.authorization.core.service.IApiResourceService;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceSyncMode;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceSyncResult;
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
        Map<String, ApiResource> protectedResources = protectedResources(resources);
        List<ApiResourceRegisterCommand> commands = resources.stream()
                .map(this::toCommand)
                .toList();
        apiResourceService.registerApiResources(commands);
        restoreProtectedResources(protectedResources);
        Map<String, ResourceSyncResult> results = new LinkedHashMap<>();
        for (ResourceDeclaration resource : resources) {
            ApiResource entity = find(toCommand(resource));
            Long targetId = entity == null ? null : entity.getId();
            results.put(resource.getId(),
                    ResourceSyncResult.of(targetId, "authorization_api_resource", "api resource synced"));
        }
        return results;
    }

    private Map<String, ApiResource> protectedResources(List<ResourceDeclaration> resources) {
        Map<String, ApiResource> protectedResources = new LinkedHashMap<>();
        for (ResourceDeclaration resource : resources) {
            if (resource.getSyncMode() == ResourceSyncMode.AUTO) {
                continue;
            }
            ApiResource entity = find(toCommand(resource));
            if (entity != null) {
                protectedResources.put(resource.getId(), entity);
            }
        }
        return protectedResources;
    }

    private void restoreProtectedResources(Map<String, ApiResource> protectedResources) {
        for (ApiResource entity : protectedResources.values()) {
            apiResourceMapper.updateById(entity);
        }
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        Long targetId = longField(resource, "targetId");
        ApiResource entity = targetId == null ? find(toCommand(resource)) : apiResourceMapper.selectById(targetId);
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

    private ApiResource find(ApiResourceRegisterCommand command) {
        return apiResourceMapper.selectOne(new LambdaQueryWrapper<ApiResource>()
                .eq(ApiResource::getModuleName, command.getModuleName())
                .eq(ApiResource::getHttpMethod, command.getHttpMethod())
                .eq(ApiResource::getPathPattern, command.getPathPattern())
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
