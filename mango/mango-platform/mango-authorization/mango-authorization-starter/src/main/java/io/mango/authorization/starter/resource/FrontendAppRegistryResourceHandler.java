package io.mango.authorization.starter.resource;

import io.mango.authorization.api.AuthorizationResourceTypes;
import io.mango.authorization.core.entity.FrontendAppRegistry;
import io.mango.authorization.core.service.IAuthorizationAppService;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resource Registry handler for authorization_frontend_app_registry.
 */
@Component
@RequiredArgsConstructor
public class FrontendAppRegistryResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "authorization_frontend_app_registry";

    private final IAuthorizationAppService appService;

    @Override
    public String resourceType() {
        return AuthorizationResourceTypes.FRONTEND_APP_REGISTRY;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("appCode")
                .fieldDescription("appCode", "前端运行单元编码，例如 guarantee-local。")
                .fieldDescription("appType", "前端入口类型：LOCAL/MICRO_APP/IFRAME/EXTERNAL_LINK。")
                .fieldDescription("deployMode", "部署模式：EMBEDDED/REMOTE/HYBRID。")
                .fieldDescription("entryUrl", "远程入口地址。")
                .fieldDescription("mountPath", "主框架挂载路径。")
                .fieldDescription("activeRule", "运行单元激活规则。")
                .fieldDescription("framework", "前端运行框架。")
                .fieldDescription("version", "运行单元版本。")
                .fieldDescription("healthCheckUrl", "健康检查地址。")
                .fieldDescription("sandboxEnabled", "是否启用沙箱。")
                .fieldDescription("styleIsolation", "样式隔离模式：NONE/SCOPED/SHADOW_DOM/IFRAME。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        FrontendAppRegistry registry = new FrontendAppRegistry();
        registry.setAppCode(requiredString(resource, "appCode"));
        registry.setAppType(stringField(resource, "appType"));
        registry.setDeployMode(stringField(resource, "deployMode"));
        registry.setEntryUrl(stringField(resource, "entryUrl"));
        registry.setMountPath(stringField(resource, "mountPath"));
        registry.setActiveRule(stringField(resource, "activeRule"));
        registry.setFramework(stringField(resource, "framework"));
        registry.setVersion(stringField(resource, "version"));
        registry.setHealthCheckUrl(stringField(resource, "healthCheckUrl"));
        registry.setSandboxEnabled(booleanField(resource, "sandboxEnabled"));
        registry.setStyleIsolation(stringField(resource, "styleIsolation"));
        Long targetId = appService.saveFrontendAppRegistry(registry);
        return ResourceSyncResult.of(targetId, TARGET_TABLE,
                "Frontend app registry synced: " + registry.getAppCode());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        Long targetId = longField(resource, "targetId");
        String appCode = targetId == null ? requiredString(resource, "appCode") : null;
        Boolean changed = targetId == null
                ? appService.deleteFrontendAppRegistry(appCode)
                : appService.deleteFrontendAppRegistry(targetId);
        return ResourceSyncResult.of(null, TARGET_TABLE,
                "Frontend app registry disabled: " + targetLabel(targetId, appCode)
                        + ", changed=" + Boolean.TRUE.equals(changed));
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        Long targetId = longField(resource, "targetId");
        String appCode = targetId == null ? requiredString(resource, "appCode") : null;
        Boolean changed = targetId == null
                ? appService.deleteFrontendAppRegistry(appCode)
                : appService.deleteFrontendAppRegistry(targetId);
        return ResourceSyncResult.of(null, TARGET_TABLE,
                "Frontend app registry deleted: " + targetLabel(targetId, appCode)
                        + ", changed=" + Boolean.TRUE.equals(changed));
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

    private Boolean booleanField(ResourceDeclaration resource, String fieldName) {
        Object value = fieldValue(resource, fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.valueOf(String.valueOf(value));
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

    private String targetLabel(Long targetId, String appCode) {
        return targetId == null ? appCode : "targetId=" + targetId;
    }

    private Object fieldValue(ResourceDeclaration resource, String fieldName) {
        ResourceField field = resource.getFields().get(fieldName);
        return field == null ? null : field.getValue();
    }
}
