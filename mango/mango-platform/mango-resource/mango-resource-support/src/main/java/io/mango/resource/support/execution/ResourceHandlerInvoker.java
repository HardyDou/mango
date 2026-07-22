package io.mango.resource.support.execution;

import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.resource.api.enums.ResourceCode;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.support.model.ResourceSyncResult;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Invokes resource handlers within their declared tenant execution scope.
 */
public class ResourceHandlerInvoker {

    /**
     * Upserts one declaration in its handler-defined tenant scope.
     */
    public ResourceSyncResult upsert(ResourceHandler handler, ResourceDeclaration declaration) {
        return inScope(handler, declaration, () -> handler.upsert(declaration));
    }

    /**
     * Upserts declarations grouped by tenant while preserving encounter order.
     */
    public Map<String, ResourceSyncResult> upsertBatch(ResourceHandler handler,
                                                       List<ResourceDeclaration> declarations) {
        String tenantField = handler.executionTenantField();
        if (!StringUtils.hasText(tenantField)) {
            return handler.upsertBatch(declarations);
        }
        Map<String, List<ResourceDeclaration>> declarationsByTenant = new LinkedHashMap<>();
        for (ResourceDeclaration declaration : declarations) {
            declarationsByTenant.computeIfAbsent(tenantId(declaration, tenantField), ignored -> new ArrayList<>())
                    .add(declaration);
        }
        Map<String, ResourceSyncResult> results = new LinkedHashMap<>();
        declarationsByTenant.forEach((tenantId, tenantDeclarations) ->
                withTenant(tenantId, () -> {
                    Map<String, ResourceSyncResult> tenantResults = handler.upsertBatch(tenantDeclarations);
                    Require.notNull(tenantResults, ResourceCode.RESOURCE_SYNC_FAILED,
                            "资源处理器未返回租户批量同步结果: " + handler.resourceType());
                    tenantResults.forEach((resourceId, result) -> {
                        ResourceSyncResult previous = results.put(resourceId, result);
                        Require.isTrue(previous == null, ResourceCode.RESOURCE_CONFLICT,
                                "资源处理器跨租户返回重复资源ID: " + resourceId);
                    });
                    return null;
                }));
        return results;
    }

    /**
     * Disables one declaration in its handler-defined tenant scope.
     */
    public ResourceSyncResult disable(ResourceHandler handler, ResourceDeclaration declaration) {
        return inScope(handler, declaration, () -> handler.disable(declaration));
    }

    /**
     * Deletes one declaration in its handler-defined tenant scope.
     */
    public ResourceSyncResult delete(ResourceHandler handler, ResourceDeclaration declaration) {
        return inScope(handler, declaration, () -> handler.delete(declaration));
    }

    private <T> T inScope(ResourceHandler handler, ResourceDeclaration declaration, Supplier<T> action) {
        String tenantField = handler.executionTenantField();
        if (!StringUtils.hasText(tenantField)) {
            return action.get();
        }
        return withTenant(tenantId(declaration, tenantField), action);
    }

    private String tenantId(ResourceDeclaration declaration, String tenantField) {
        Require.notNull(declaration, ResourceCode.RESOURCE_INVALID, "资源声明不能为空");
        Map<String, ResourceField> fields = declaration.getFields();
        Require.notNull(fields, ResourceCode.RESOURCE_INVALID,
                "租户资源声明字段不能为空: " + declaration.getId());
        ResourceField field = fields.get(tenantField);
        Require.notNull(field, ResourceCode.RESOURCE_INVALID,
                "租户资源声明缺少字段 " + tenantField + ": " + declaration.getId());
        String tenantId = field.getValue() == null ? "" : String.valueOf(field.getValue()).trim();
        Require.notBlank(tenantId, ResourceCode.RESOURCE_INVALID,
                "租户资源声明字段不能为空 " + tenantField + ": " + declaration.getId());
        return tenantId;
    }

    private <T> T withTenant(String tenantId, Supplier<T> action) {
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previous.withTenantId(tenantId));
            return action.get();
        } finally {
            MangoContextHolder.set(previous);
        }
    }
}
