package io.mango.authorization.starter.controller;

import io.mango.authorization.api.FrontendAppHealthApi;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.command.FrontendAppHealthCheckCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.vo.FrontendAppHealthCheckVO;
import io.mango.authorization.core.service.IFrontendAppHealthService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前端运行单元健康检查控制器。
 */
@RestController
@RequestMapping("/authorization/frontend-apps")
@RequiredArgsConstructor
@Validated
@Tag(name = "前端运行单元", description = "微应用运行单元健康检查接口")
public class FrontendAppHealthController implements FrontendAppHealthApi {

    private final IFrontendAppHealthService service;

    @Override
    @PostMapping("/health-check")
    @Operation(summary = "检测前端运行单元", description = "只探测注册表登记的健康检查或入口地址，默认拒绝私网目标")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "authorization:app:query")
    public R<FrontendAppHealthCheckVO> check(@RequestBody FrontendAppHealthCheckCommand command) {
        return R.ok(service.check(command));
    }
}
