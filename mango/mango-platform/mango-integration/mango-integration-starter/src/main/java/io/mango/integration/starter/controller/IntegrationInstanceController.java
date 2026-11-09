package io.mango.integration.starter.controller;

import io.mango.integration.api.IntegrationInstanceApi;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.integration.api.command.IntegrationInstanceCommand;
import io.mango.integration.api.command.IntegrationConnectionTestCommand;
import io.mango.integration.api.command.IntegrationInstanceStatusCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.integration.api.query.IntegrationInstancePageQuery;
import io.mango.integration.api.vo.IntegrationCallLogVO;
import io.mango.integration.api.vo.IntegrationConnectionTestVO;
import io.mango.integration.api.vo.IntegrationInstanceVO;
import io.mango.integration.core.service.IIntegrationInstanceService;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 集成连接实例控制器。
 */
@RestController
@RequestMapping("/integration/instances")
@RequiredArgsConstructor
@Validated
@Tag(name = "集成连接实例", description = "外部系统连接实例管理接口，配置按连接器规范校验")
public class IntegrationInstanceController implements IntegrationInstanceApi {

    private final IIntegrationInstanceService service;

    @Override
    @GetMapping("/page")
    @Operation(summary = "分页查询连接实例", description = "按当前租户分页返回实例，包含回调路径与能力")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "integration:instance:list")
    public R<PageResult<IntegrationInstanceVO>> page(@ParameterObject IntegrationInstancePageQuery query) {
        return R.ok(service.page(query));
    }

    @Override
    @GetMapping("/detail")
    @Operation(summary = "查询连接实例详情", description = "返回配置值、凭证编码、能力与回调路径")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "integration:instance:query")
    public R<IntegrationInstanceVO> detail(
            @Parameter(description = "实例编码")
            @RequestParam(name = "instanceCode") String instanceCode) {
        return R.ok(service.detail(instanceCode));
    }

    @Override
    @PostMapping
    @Operation(summary = "保存连接实例", description = "校验配置规范与能力清单，启用前必须绑定可用凭证")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "integration:instance:add")
    public R<Boolean> save(@RequestBody IntegrationInstanceCommand command) {
        return R.ok(service.save(command));
    }

    @Override
    @PostMapping("/test-connection")
    @Operation(summary = "连接测试", description = "按实例声明读取配置与凭证并调用目标接口，结果不含凭证")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "integration:instance:query")
    public R<IntegrationConnectionTestVO> testConnection(
            @RequestBody IntegrationConnectionTestCommand command) {
        return R.ok(service.testConnection(command));
    }

    @Override
    @GetMapping("/call-logs")
    @Operation(summary = "查询实例调用记录", description = "按实例倒序返回最近调用记录，不含请求体和凭证")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "integration:instance:query")
    public R<List<IntegrationCallLogVO>> callLogs(
            @Parameter(description = "实例编码")
            @RequestParam(name = "instanceCode") String instanceCode,
            @Parameter(description = "返回条数，默认50，最大200")
            @RequestParam(name = "limit", required = false) Integer limit) {
        return R.ok(service.callLogs(instanceCode, limit));
    }

    @Override
    @PostMapping("/change-status")
    @Operation(summary = "启用或停用连接实例", description = "未绑定凭证的实例不允许启用")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "integration:instance:edit")
    public R<Boolean> changeStatus(@RequestBody IntegrationInstanceStatusCommand command) {
        return R.ok(service.changeStatus(command));
    }
}
