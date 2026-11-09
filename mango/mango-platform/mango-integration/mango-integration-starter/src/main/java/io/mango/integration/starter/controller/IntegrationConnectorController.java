package io.mango.integration.starter.controller;

import io.mango.integration.api.IntegrationConnectorApi;
import io.mango.integration.api.command.IntegrationConnectorCommand;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.integration.api.query.IntegrationConnectorPageQuery;
import io.mango.integration.api.vo.IntegrationConnectorVO;
import io.mango.integration.core.service.IIntegrationConnectorService;
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

/**
 * 集成连接器定义控制器。
 */
@RestController
@RequestMapping("/integration/connectors")
@RequiredArgsConstructor
@Validated
@Tag(name = "集成连接器", description = "连接器定义只读查询接口，定义由平台随版本发布")
public class IntegrationConnectorController implements IntegrationConnectorApi {

    private final IIntegrationConnectorService service;

    @Override
    @GetMapping("/page")
    @Operation(summary = "分页查询连接器定义", description = "只读查询平台已发布的连接器定义及其被引用实例数")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "integration:connector:list")
    public R<PageResult<IntegrationConnectorVO>> page(@ParameterObject IntegrationConnectorPageQuery query) {
        return R.ok(service.page(query));
    }

    @Override
    @GetMapping("/detail")
    @Operation(summary = "查询连接器定义详情", description = "返回配置规范、认证规范、操作与能力清单")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "integration:connector:query")
    public R<IntegrationConnectorVO> detail(
            @Parameter(description = "连接器编码")
            @RequestParam(name = "connectorCode") String connectorCode) {
        return R.ok(service.detail(connectorCode));
    }

    @Override
    @PostMapping
    @Operation(summary = "保存连接器定义", description = "草稿可修改，已发布版本不可改，需要变更时提升版本号")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "integration:connector:edit")
    public R<Boolean> save(@RequestBody IntegrationConnectorCommand command) {
        return R.ok(service.save(command));
    }
}
