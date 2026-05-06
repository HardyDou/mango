package io.mango.authorization.core.service;

import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;

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
}
