package io.mango.resource.core.service;

import io.mango.common.vo.PageResult;
import io.mango.resource.api.query.ResourceLogPageQuery;
import io.mango.resource.api.query.ResourceRegistryPageQuery;
import io.mango.resource.api.vo.ResourceChangeLogVO;
import io.mango.resource.api.vo.ResourceHandlerSpecVO;
import io.mango.resource.api.vo.ResourceRegistryVO;
import io.mango.resource.api.vo.ResourceSyncLogVO;

import java.util.List;

/**
 * 资源注册中心管理服务。
 */
public interface IResourceAdminService {

    PageResult<ResourceRegistryVO> pageRegistries(ResourceRegistryPageQuery query);

    Boolean forceSync();

    Boolean deleteResource(String resourceId, Boolean physical);

    PageResult<ResourceSyncLogVO> pageSyncLogs(ResourceLogPageQuery query);

    PageResult<ResourceChangeLogVO> pageChangeLogs(ResourceLogPageQuery query);

    List<ResourceHandlerSpecVO> listHandlerSpecs();
}
