package io.mango.authorization.core.service;

import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.command.ApiResourceManualCommand;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.query.ApiResourceManualQuery;
import io.mango.authorization.api.query.ApiResourcePageQuery;
import io.mango.authorization.api.vo.ApiResourceFilterOptionsVO;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.authorization.api.vo.ApiResourceVO;
import io.mango.common.vo.PageResult;

import java.util.List;

/**
 * API 资源内部服务。
 *
 * @author hardy
 */
public interface IApiResourceService {

    /**
     * 注册扫描到的 API 资源。
     *
     * @param resources 待注册资源
     * @return 注册结果
     */
    ApiResourceRegisterResultVO registerApiResources(List<ApiResourceRegisterCommand> resources);

    /**
     * Creates or updates only the supplied API resources without treating omitted resources as stale.
     */
    ApiResourceRegisterResultVO upsertApiResources(List<ApiResourceRegisterCommand> resources);

    /**
     * Physically removes one API resource and invalidates runtime decision caches.
     *
     * @param resourceId target API resource ID
     * @return whether a row was removed
     */
    boolean deleteApiResource(Long resourceId);

    /**
     * 根据请求方法和路径解析运行时访问决策。
     *
     * @param httpMethod HTTP 方法
     * @param path 请求路径
     * @return 访问决策
     */
    ApiResourceAccessDecisionVO resolveAccessDecision(String httpMethod, String path);

    /**
     * 清理运行时资源缓存。
     */
    void refreshRuntimeCache();

    /**
     * 分页查询 API 资源登记明细。
     *
     * @param query 分页与筛选条件
     * @return API 资源分页结果
     */
    PageResult<ApiResourceVO> pageApiResources(ApiResourcePageQuery query);

    /**
     * 查询 API 资源的模块与领域筛选维度。
     *
     * @return 筛选维度
     */
    ApiResourceFilterOptionsVO listApiResourceFilterOptions();

    /**
     * 人工接管或更新同一路由的访问模式，接管记录在运行时优先于其它来源。
     *
     * @param command 人工接管命令
     */
    void saveManualApiResource(ApiResourceManualCommand command);

    /**
     * 释放人工接管，使 Controller 扫描或固定声明可以重新生效。
     *
     * @param query 目标路由
     */
    void releaseManualApiResource(ApiResourceManualQuery query);
}
