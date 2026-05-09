package io.mango.guarantee.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.guarantee.api.command.GuaranteeCaseCommand;
import io.mango.guarantee.api.query.GuaranteeCaseQuery;
import io.mango.guarantee.api.vo.GuaranteeCaseVO;
import io.mango.guarantee.core.service.IGuaranteeCaseService;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 保函业务单管理控制器。
 */
@RestController
@RequestMapping("/guarantee/cases")
@RequiredArgsConstructor
@Tag(name = "保函业务单", description = "保函业务单列表、详情、新增、修改、删除接口")
public class GuaranteeCaseController {

    private final IGuaranteeCaseService caseService;

    @GetMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "分页查询保函业务单", description = "登录接口。返回当前机构创建或作为参与方可见的保函业务单")
    public R<PersistencePageResult<GuaranteeCaseVO>> page(@ParameterObject GuaranteeCaseQuery query) {
        return R.ok(caseService.page(query));
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "获取保函业务单详情", description = "登录接口。按业务单ID查询当前机构可见的保函业务单详情")
    public R<GuaranteeCaseVO> get(
            @Parameter(description = "业务单ID")
            @RequestParam Long caseId) {
        return R.ok(caseService.get(caseId));
    }

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "创建保函业务单", description = "登录接口。当前机构作为来源机构创建保函业务单")
    public R<Long> create(@Valid @RequestBody GuaranteeCaseCommand command) {
        return R.ok(caseService.create(command));
    }

    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "更新保函业务单", description = "登录接口。仅来源机构可更新保函业务单")
    public R<Boolean> update(@Valid @RequestBody GuaranteeCaseCommand command) {
        return R.ok(caseService.update(command));
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "删除保函业务单", description = "登录接口。仅来源机构可删除保函业务单")
    public R<Boolean> delete(
            @Parameter(description = "业务单ID")
            @RequestParam Long caseId) {
        return R.ok(caseService.delete(caseId));
    }
}
