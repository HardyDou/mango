package io.mango.authorization.api;

import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.common.result.R;

import java.util.List;

/**
 * API 资源远程契约。
 *
 * @author hardy
 */
public interface ApiResourceApi {

    /**
     * 注册服务扫描到的 API 资源。
     *
     * @param resources API 资源列表
     * @return 注册结果
     */
    R<ApiResourceRegisterResultVO> registerApiResources(
            List<ApiResourceRegisterCommand> resources);

    /**
     * 解析 HTTP 请求的访问控制决策。
     *
     * @param httpMethod HTTP 方法
     * @param path 请求路径
     * @return 访问控制决策
     */
    R<ApiResourceAccessDecisionVO> resolveAccessDecision(
            String httpMethod,
            String path);
}
