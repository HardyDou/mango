package io.mango.authorization.api;

import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

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
    @Operation(summary = "注册 API 资源", description = "注册服务启动时扫描到的 API 资源定义")
    R<ApiResourceRegisterResultVO> registerApiResources(
            List<ApiResourceRegisterCommand> resources);

    /**
     * 解析 HTTP 请求的访问控制决策。
     *
     * @param httpMethod HTTP 方法
     * @param path 请求路径
     * @return 访问控制决策
     */
    @Operation(summary = "解析 API 访问决策", description = "根据 HTTP 方法和请求路径解析访问模式与权限码")
    R<ApiResourceAccessDecisionVO> resolveAccessDecision(
            @Parameter(description = "HTTP 方法，如 GET、POST") String httpMethod,
            @Parameter(description = "请求路径") String path);
}
