package io.mango.authorization.starter.resource;

import io.mango.authorization.api.command.FrontendModuleRuntimeStrategyCommand;
import io.mango.authorization.core.service.IFrontendRuntimeStrategyService;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resource Registry handler for authorization_frontend_module_runtime_strategy.
 */
@Component
@RequiredArgsConstructor
public class FrontendModuleRuntimeStrategyResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "authorization_frontend_module_runtime_strategy";

    private final IFrontendRuntimeStrategyService runtimeStrategyService;

    @Override
    public String resourceType() {
        return ResourceTypes.FRONTEND_MODULE_RUNTIME_STRATEGY;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("appCode")
                .requiredField("deployProfile")
                .requiredField("pageType")
                .requiredField("runtimeCode")
                .fieldDescription("appCode", "逻辑应用编码，例如 internal-admin。")
                .fieldDescription("moduleCode", "能力模块编码，例如 guarantee。")
                .fieldDescription("deployProfile", "部署配置档：monolith/hybrid/micro 或业务自定义 profile。")
                .fieldDescription("pageType", "页面运行类型：LOCAL_ROUTE/MICRO_ROUTE/IFRAME/EXTERNAL_LINK。")
                .fieldDescription("runtimeCode", "前端运行单元编码，关联 authorization_frontend_app_registry.app_code。")
                .fieldDescription("status", "状态：0-停用，1-启用。")
                .fieldDescription("sort", "排序号。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        FrontendModuleRuntimeStrategyCommand command = toCommand(resource);
        Long targetId = runtimeStrategyService.save(command);
        return ResourceSyncResult.of(targetId, TARGET_TABLE,
                "Frontend module runtime strategy synced: "
                        + command.getAppCode() + "/" + command.getModuleCode() + "/" + command.getDeployProfile());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        Long targetId = longField(resource, "targetId");
        StrategyKey key = targetId == null ? toKey(resource) : null;
        Boolean changed = targetId == null
                ? runtimeStrategyService.disable(key.appCode(), key.moduleCode(), key.deployProfile())
                : runtimeStrategyService.disable(targetId);
        return ResourceSyncResult.of(null, TARGET_TABLE,
                "Frontend module runtime strategy disabled: "
                        + targetLabel(targetId, key)
                        + ", changed=" + Boolean.TRUE.equals(changed));
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        Long targetId = longField(resource, "targetId");
        StrategyKey key = targetId == null ? toKey(resource) : null;
        Boolean changed = targetId == null
                ? runtimeStrategyService.delete(key.appCode(), key.moduleCode(), key.deployProfile())
                : runtimeStrategyService.delete(targetId);
        return ResourceSyncResult.of(null, TARGET_TABLE,
                "Frontend module runtime strategy deleted: "
                        + targetLabel(targetId, key)
                        + ", changed=" + Boolean.TRUE.equals(changed));
    }

    private FrontendModuleRuntimeStrategyCommand toCommand(ResourceDeclaration resource) {
        FrontendModuleRuntimeStrategyCommand command = new FrontendModuleRuntimeStrategyCommand();
        command.setAppCode(requiredString(resource, "appCode"));
        command.setModuleCode(defaultString(stringField(resource, "moduleCode"), resource.getModuleCode()));
        command.setDeployProfile(requiredString(resource, "deployProfile"));
        command.setPageType(requiredString(resource, "pageType"));
        command.setRuntimeCode(requiredString(resource, "runtimeCode"));
        command.setStatus(intField(resource, "status"));
        command.setSort(intField(resource, "sort"));
        return command;
    }

    private StrategyKey toKey(ResourceDeclaration resource) {
        return new StrategyKey(
                requiredString(resource, "appCode"),
                defaultString(stringField(resource, "moduleCode"), resource.getModuleCode()),
                requiredString(resource, "deployProfile")
        );
    }

    private record StrategyKey(String appCode, String moduleCode, String deployProfile) {
    }

    private String requiredString(ResourceDeclaration resource, String fieldName) {
        String value = stringField(resource, fieldName);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(resourceType() + " field is required: " + fieldName);
        }
        return value.trim();
    }

    private String stringField(ResourceDeclaration resource, String fieldName) {
        Object value = fieldValue(resource, fieldName);
        return value == null ? null : String.valueOf(value);
    }

    private Integer intField(ResourceDeclaration resource, String fieldName) {
        Object value = fieldValue(resource, fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private Long longField(ResourceDeclaration resource, String fieldName) {
        Object value = fieldValue(resource, fieldName);
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private Object fieldValue(ResourceDeclaration resource, String fieldName) {
        ResourceField field = resource.getFields().get(fieldName);
        return field == null ? null : field.getValue();
    }

    private String defaultString(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String targetLabel(Long targetId, StrategyKey key) {
        return targetId == null ? key.appCode() + "/" + key.moduleCode() : "targetId=" + targetId;
    }
}
