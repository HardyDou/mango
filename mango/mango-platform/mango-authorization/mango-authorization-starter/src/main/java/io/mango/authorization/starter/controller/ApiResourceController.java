package io.mango.authorization.starter.controller;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.authorization.core.service.IApiResourceService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API 资源远程端点。
 *
 * @author hardy
 */
@RestController
@RequestMapping("/authorization")
@RequiredArgsConstructor
public class ApiResourceController implements ApiResourceApi {

    private final IApiResourceService apiResourceService;

    @PostMapping("/api-resources/register")
    @Operation(summary = "注册 API 资源", description = "注册服务启动时扫描到的 API 资源定义")
    @Override
    public R<ApiResourceRegisterResultVO> registerApiResources(
            @RequestBody List<ApiResourceRegisterCommand> resources) {
        return R.ok(apiResourceService.registerApiResources(resources));
    }

    @GetMapping("/api-resources/access-decision")
    @Operation(summary = "解析 API 访问决策", description = "根据 HTTP 方法和请求路径解析访问模式与权限码")
    @Override
    public R<ApiResourceAccessDecisionVO> resolveAccessDecision(
            @Parameter(description = "HTTP 方法，如 GET、POST") @RequestParam String httpMethod,
            @Parameter(description = "请求路径") @RequestParam String path) {
        return R.ok(apiResourceService.resolveAccessDecision(httpMethod, path));
    }
}
