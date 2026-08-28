package io.mango.resource.support;

import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceHandlerSpec;
import io.mango.resource.support.model.ResourceSyncContext;
import io.mango.resource.support.model.ResourceSyncResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 资源目标模块处理器。
 */
public interface ResourceHandler {

    /**
     * 处理器支持的资源类型。
     *
     * @return 资源类型。
     */
    String resourceType();

    /**
     * 当前资源类型在批量同步前依赖的资源类型。
     * <p>
     * Resource Registry 会在同一批次内按这些依赖做拓扑排序。未出现在本批次内的依赖不强制同步，
     * 目标资源是否已存在仍由具体 handler 校验。
     *
     * @return 前置资源类型列表。
     */
    default List<String> dependsOnResourceTypes() {
        return List.of();
    }

    /**
     * 处理器支持的字段契约。
     *
     * @return 字段契约。
     */
    default ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .build();
    }

    /**
     * Declaration field that defines the tenant execution scope for this handler.
     *
     * <p>When present, Resource executors group batch operations by this field,
     * switch {@code MangoContextHolder} before invoking the handler, and restore
     * the previous context afterward.</p>
     *
     * @return tenant field name, or an empty string for non-tenant-scoped handlers
     */
    default String executionTenantField() {
        return "";
    }

    /**
     * 创建或更新目标资源。
     *
     * @param resource 资源声明。
     * @return 同步结果。
     */
    ResourceSyncResult upsert(ResourceDeclaration resource);

    /**
     * 批量处理时是否需要收到当前类型的完整 active 声明集合。
     * <p>
     * 目标模块如果会基于“本次扫描批次”禁用缺失资源，需要返回 {@code true}。
     *
     * @return 是否需要完整批次。
     */
    default boolean requiresCompleteBatch() {
        return false;
    }

    /**
     * 批量创建或更新目标资源。
     * <p>
     * 默认逐条调用 {@link #upsert(ResourceDeclaration)}。目标模块如果存在
     * “一次扫描批次”语义，应覆盖该方法，避免逐条处理改变原有数据逻辑。
     *
     * @param resources 资源声明列表。
     * @return 按资源 ID 返回的同步结果。
     */
    default Map<String, ResourceSyncResult> upsertBatch(List<ResourceDeclaration> resources) {
        Map<String, ResourceSyncResult> results = new LinkedHashMap<>();
        for (ResourceDeclaration resource : resources) {
            results.put(resource.getId(), upsert(resource));
        }
        return results;
    }

    /**
     * Synchronizes only declarations selected by the Registry while exposing the complete type batch
     * for dependency resolution and one fixed timestamp per selected Resource.
     *
     * <p>Handlers that opt into runtime-change preservation must override this method, inspect their
     * own target state, and return {@code PRESERVED} without writing when the target was modified after
     * the previous synchronization. The legacy default keeps existing Handler behavior.</p>
     *
     * @param declarations declarations whose source hash changed
     * @param completeBatch complete active declarations of this Resource type
     * @param syncContexts synchronization context keyed by Resource ID
     * @return results keyed by Resource ID
     */
    default Map<String, ResourceSyncResult> upsertBatchWithContext(
            List<ResourceDeclaration> declarations,
            List<ResourceDeclaration> completeBatch,
            Map<String, ResourceSyncContext> syncContexts) {
        return upsertBatch(requiresCompleteBatch() ? completeBatch : declarations);
    }

    /**
     * 逻辑禁用目标资源。
     *
     * @param resource 资源声明。
     * @return 同步结果。
     */
    ResourceSyncResult disable(ResourceDeclaration resource);

    /**
     * 物理删除目标资源。
     * <p>
     * 默认降级为逻辑禁用，目标模块如果支持物理删除，应自行覆盖该方法。
     *
     * @param resource 资源声明。
     * @return 同步结果。
     */
    default ResourceSyncResult delete(ResourceDeclaration resource) {
        return disable(resource);
    }
}
