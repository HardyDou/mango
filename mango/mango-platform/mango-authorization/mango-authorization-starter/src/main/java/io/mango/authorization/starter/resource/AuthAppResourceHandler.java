package io.mango.authorization.starter.resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.command.AppCommand;
import io.mango.authorization.api.command.AppLoginContextCommand;
import io.mango.authorization.api.vo.AppLoginContextVO;
import io.mango.authorization.api.vo.AppVO;
import io.mango.authorization.core.service.IAuthorizationAppService;
import io.mango.authorization.core.support.AuthorizationResourceIds;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceHandlerSpec;
import io.mango.resource.support.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Synchronizes required logical applications and login contexts without creating frontend runtime units.
 */
@Component
@RequiredArgsConstructor
public class AuthAppResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "authorization_app";

    private final IAuthorizationAppService appService;
    private final ObjectMapper objectMapper;
    private final ResourceFieldReader fields = new ResourceFieldReader(ResourceTypes.AUTH_APP);

    @Override
    public String resourceType() {
        return ResourceTypes.AUTH_APP;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("appCode")
                .requiredField("appName")
                .requiredField("loginContexts")
                .fieldDescription("loginContexts", "应用登录上下文列表，每项包含 realm、actorType、defaultFlag、status 和 sort。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        AppCommand command = toCommand(resource, null);
        Long appId = appService.upsertBaseline(command);
        return ResourceSyncResult.of(appId, TARGET_TABLE, "Auth app synced: " + command.getAppCode());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        String appCode = fields.requiredString(resource, "appCode");
        AppVO existing = appService.getByAppCode(appCode);
        if (existing == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Auth app disabled: changed=false");
        }
        AppCommand command = toCommand(resource, existing);
        command.setStatus(0);
        Long appId = appService.upsertBaseline(command);
        return ResourceSyncResult.of(appId, TARGET_TABLE, "Auth app disabled: changed=true");
    }

    private AppCommand toCommand(ResourceDeclaration resource, AppVO existing) {
        AppCommand command = new AppCommand();
        command.setAppId(existing == null
                ? AuthorizationResourceIds.declaredOrStable(
                        fields.longField(resource, "targetId"), TARGET_TABLE,
                        fields.requiredString(resource, "appCode"))
                : existing.getAppId());
        command.setAppCode(fields.requiredString(resource, "appCode"));
        command.setAppName(fields.stringField(resource, "appName", existing == null ? null : existing.getAppName()));
        if (command.getAppName() == null || command.getAppName().isBlank()) {
            throw new IllegalStateException(ResourceTypes.AUTH_APP + " field is required: appName");
        }
        command.setIcon(fields.stringField(resource, "icon", existing == null ? null : existing.getIcon()));
        command.setSort(fields.intField(resource, "sort",
                existing == null || existing.getSort() == null ? 0 : existing.getSort()));
        command.setStatus(statusValue(resource, existing));
        command.setRemark(fields.stringField(resource, "remark", existing == null ? null : existing.getRemark()));
        command.setLoginContexts(readLoginContexts(resource, existing, command.getAppCode()));
        return command;
    }

    private List<AppLoginContextCommand> readLoginContexts(
            ResourceDeclaration resource, AppVO existing, String appCode) {
        Object value = fields.fieldValue(resource, "loginContexts");
        if (value != null) {
            List<AppLoginContextCommand> contexts = objectMapper.convertValue(
                    value, new TypeReference<List<AppLoginContextCommand>>() { });
            if (!contexts.isEmpty()) {
                contexts.forEach(context -> assignStableContextId(context, appCode));
                return contexts;
            }
        }
        if (existing != null && existing.getLoginContexts() != null && !existing.getLoginContexts().isEmpty()) {
            return existing.getLoginContexts().stream().map(this::toCommand).toList();
        }
        throw new IllegalStateException(ResourceTypes.AUTH_APP + " field is required: loginContexts");
    }

    private void assignStableContextId(AppLoginContextCommand context, String appCode) {
        if (context.getContextId() == null) {
            context.setContextId(AuthorizationResourceIds.stable(
                    "authorization_app_login_context", appCode,
                    context.getRealm(), context.getActorType()));
        }
    }

    private AppLoginContextCommand toCommand(AppLoginContextVO source) {
        AppLoginContextCommand target = new AppLoginContextCommand();
        target.setContextId(source.getContextId());
        target.setRealm(source.getRealm());
        target.setActorType(source.getActorType());
        target.setDefaultFlag(source.getDefaultFlag());
        target.setStatus(source.getStatus());
        target.setSort(source.getSort());
        return target;
    }

    private Integer statusValue(ResourceDeclaration resource, AppVO existing) {
        Integer status = fields.intField(resource, "status", null);
        if (status != null) {
            return status;
        }
        if (resource.getStatus() == ResourceStatus.DISABLED) {
            return 0;
        }
        return existing == null || existing.getStatus() == null ? 1 : existing.getStatus();
    }
}
