package io.mango.resource.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.resource.api.query.ResourceLogPageQuery;
import io.mango.resource.api.query.ResourceRegistryPageQuery;
import io.mango.resource.api.vo.ResourceChangeLogVO;
import io.mango.resource.api.vo.ResourceHandlerSpecVO;
import io.mango.resource.api.vo.ResourceRegistryVO;
import io.mango.resource.api.vo.ResourceSyncLogVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 资源注册中心管理 API 契约。
 */
public interface ResourceAdminApi {

    /**
     * 分页查询注册资源。
     *
     * @param query 分页条件。
     * @return 注册资源分页结果。
     */
    R<PageResult<ResourceRegistryVO>> pageRegistries(@Valid ResourceRegistryPageQuery query);

    /**
     * 强制执行本地资源同步。
     *
     * @return 是否已完成同步。
     */
    R<Boolean> forceSync();

    /**
     * 删除或禁用注册资源。
     *
     * @param resourceId 稳定资源ID。
     * @param physical 是否物理删除目标数据。
     * @return 是否处理成功。
     */
    R<Boolean> deleteResource(@NotBlank(message = "资源ID不能为空") String resourceId,
                              @NotNull(message = "删除方式不能为空") Boolean physical);

    /**
     * 分页查询资源同步记录。
     *
     * @param query 分页条件。
     * @return 同步记录分页结果。
     */
    R<PageResult<ResourceSyncLogVO>> pageSyncLogs(@Valid ResourceLogPageQuery query);

    /**
     * 分页查询资源变更记录。
     *
     * @param query 分页条件。
     * @return 变更记录分页结果。
     */
    R<PageResult<ResourceChangeLogVO>> pageChangeLogs(@Valid ResourceLogPageQuery query);

    /**
     * 查询资源处理器字段契约。
     *
     * @return 处理器字段契约。
     */
    R<List<ResourceHandlerSpecVO>> listHandlerSpecs();
}
