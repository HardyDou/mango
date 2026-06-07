package io.mango.domain.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.domain.api.DomainApi;
import io.mango.domain.api.command.CreateDomainCommand;
import io.mango.domain.api.command.UpdateDomainCommand;
import io.mango.domain.api.command.UpdateDomainStatusCommand;
import io.mango.domain.api.query.DomainPageQuery;
import io.mango.domain.api.vo.DomainVO;
import io.mango.domain.core.service.IDomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业务域管理接口。
 */
@RestController
@RequestMapping("/domain/domains")
@RequiredArgsConstructor
@Validated
@Tag(name = "业务域管理", description = "业务域维护接口")
public class DomainController implements DomainApi {

    private final IDomainService domainService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "domain:list", desc = "分页查询业务域")
    @Operation(summary = "分页查询业务域")
    public R<PageResult<DomainVO>> page(@ParameterObject DomainPageQuery query) {
        return domainService.page(query);
    }

    @Override
    @GetMapping("/tree")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "domain:list", desc = "查询业务域树")
    @Operation(summary = "查询业务域树")
    public R<List<DomainVO>> tree(@ParameterObject DomainPageQuery query) {
        return domainService.tree(query);
    }

    @Override
    @GetMapping("/enabled-tree")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "查询启用业务域树")
    @Operation(summary = "查询启用业务域树")
    public R<List<DomainVO>> enabledTree() {
        return domainService.enabledTree();
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "domain:list", desc = "查询业务域详情")
    @Operation(summary = "查询业务域详情")
    public R<DomainVO> detail(@Parameter(description = "业务域ID", required = true) @RequestParam Long id) {
        return domainService.detail(id);
    }

    @Override
    @GetMapping("/code")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "根据编码查询业务域")
    @Operation(summary = "根据编码查询业务域")
    public R<DomainVO> detailByCode(
            @Parameter(description = "业务域编码", required = true) @RequestParam String domainCode) {
        return domainService.detailByCode(domainCode);
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "domain:add", desc = "新增业务域")
    @Operation(summary = "新增业务域")
    public R<Long> create(@RequestBody CreateDomainCommand command) {
        return domainService.create(command);
    }

    @Override
    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "domain:edit", desc = "修改业务域")
    @Operation(summary = "修改业务域")
    public R<Boolean> update(@RequestBody UpdateDomainCommand command) {
        return domainService.update(command);
    }

    @Override
    @PutMapping("/status")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "domain:status", desc = "启停业务域")
    @Operation(summary = "启停业务域")
    public R<Boolean> updateStatus(@RequestBody UpdateDomainStatusCommand command) {
        return domainService.updateStatus(command);
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "domain:delete", desc = "删除业务域")
    @Operation(summary = "删除业务域")
    public R<Boolean> delete(@Parameter(description = "业务域ID", required = true) @RequestParam Long id) {
        return domainService.delete(id);
    }
}
