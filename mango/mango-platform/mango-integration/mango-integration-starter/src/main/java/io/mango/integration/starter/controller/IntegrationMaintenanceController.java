package io.mango.integration.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.integration.api.IntegrationMaintenanceApi;
import io.mango.integration.api.command.IntegrationMaintenanceCleanupCommand;
import io.mango.integration.core.service.IIntegrationMaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 集成模块维护控制器。
 */
@RestController
@RequestMapping("/integration/maintenance")
@RequiredArgsConstructor
@Validated
@Tag(name = "集成维护", description = "集成模块历史日志手动清理接口")
public class IntegrationMaintenanceController implements IntegrationMaintenanceApi {

    private final IIntegrationMaintenanceService service;

    @Override
    @PostMapping("/cleanup")
    @Operation(summary = "清理集成历史日志", description = "按保留天数删除调用记录、凭证读取记录或轮换记录")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "integration:maintenance:cleanup")
    public R<Integer> cleanup(@RequestBody IntegrationMaintenanceCleanupCommand command) {
        return R.ok(service.cleanup(command));
    }
}
