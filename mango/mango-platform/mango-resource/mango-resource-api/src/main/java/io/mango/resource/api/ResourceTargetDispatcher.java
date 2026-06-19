package io.mango.resource.api;

import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceSyncResult;

import java.util.List;
import java.util.Map;

/**
 * 资源目标模块远程调度器。
 */
public interface ResourceTargetDispatcher {

    /**
     * 判断目标模块是否可以远程调度。
     *
     * @param targetModule 资源声明目标模块。
     * @return 是否支持调度。
     */
    boolean supports(String targetModule);

    /**
     * 远程批量创建或更新资源。
     *
     * @param declarations 资源声明。
     * @param completeBatch 同类型完整 active 批次。
     * @return 按资源 ID 返回的同步结果。
     */
    Map<String, ResourceSyncResult> upsertBatch(List<ResourceDeclaration> declarations,
                                                List<ResourceDeclaration> completeBatch);

    /**
     * 远程禁用资源。
     *
     * @param declaration 资源声明。
     * @return 同步结果。
     */
    ResourceSyncResult disable(ResourceDeclaration declaration);

    /**
     * 远程删除资源。
     *
     * @param declaration 资源声明。
     * @return 同步结果。
     */
    ResourceSyncResult delete(ResourceDeclaration declaration);
}
