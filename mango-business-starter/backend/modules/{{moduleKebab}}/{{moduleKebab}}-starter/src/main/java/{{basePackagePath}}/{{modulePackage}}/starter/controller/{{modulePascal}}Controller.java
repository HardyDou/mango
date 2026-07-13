package {{basePackage}}.{{modulePackage}}.starter.controller;

import {{basePackage}}.{{modulePackage}}.api.{{modulePascal}}Api;
import {{basePackage}}.{{modulePackage}}.api.command.Create{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.command.Update{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.query.{{aggregatePascal}}PageQuery;
import {{basePackage}}.{{modulePackage}}.api.vo.{{aggregatePascal}}VO;
import {{basePackage}}.{{modulePackage}}.core.service.I{{aggregatePascal}}Service;
import io.mango.common.result.R;
import io.mango.infra.persistence.api.crud.DeleteCommand;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {{moduleName}}接口适配器。
 */
@RestController
@Validated
@Tag(name = "{{moduleName}}", description = "{{moduleName}}管理接口")
@RequestMapping("/{{moduleKebab}}/{{aggregateKebab}}s")
public class {{modulePascal}}Controller implements {{modulePascal}}Api {

    private final I{{aggregatePascal}}Service {{aggregateCamel}}Service;

    public {{modulePascal}}Controller(I{{aggregatePascal}}Service {{aggregateCamel}}Service) {
        this.{{aggregateCamel}}Service = {{aggregateCamel}}Service;
    }

    @Override
    @Operation(summary = "创建{{aggregateName}}", description = "创建一条{{aggregateName}}业务记录")
    @PostMapping("/create")
    public R<Long> create(@RequestBody @Valid Create{{aggregatePascal}}Command command) {
        return R.ok({{aggregateCamel}}Service.create(command));
    }

    @Override
    @Operation(summary = "修改{{aggregateName}}", description = "按业务标识修改{{aggregateName}}业务记录")
    @PostMapping("/update")
    public R<Boolean> update(@RequestBody @Valid Update{{aggregatePascal}}Command command) {
        return R.ok({{aggregateCamel}}Service.update(command));
    }

    @Override
    @Operation(summary = "删除{{aggregateName}}", description = "按业务标识删除{{aggregateName}}业务记录")
    @PostMapping("/delete")
    public R<Boolean> delete(@RequestBody @Valid DeleteCommand command) {
        return R.ok({{aggregateCamel}}Service.delete(command));
    }

    @Override
    @Operation(summary = "分页查询{{aggregateName}}", description = "按查询条件分页获取{{aggregateName}}")
    @GetMapping("/page")
    public R<PersistencePageResult<{{aggregatePascal}}VO>> page(
            @ParameterObject @Valid {{aggregatePascal}}PageQuery query) {
        return R.ok({{aggregateCamel}}Service.page(query));
    }

    @Override
    @Operation(summary = "查询{{aggregateName}}详情", description = "按业务标识获取{{aggregateName}}详情")
    @GetMapping("/detail")
    public R<{{aggregatePascal}}VO> detail(
            @Parameter(description = "业务标识") @RequestParam("id") @NotNull Long id) {
        return R.ok({{aggregateCamel}}Service.detail(id));
    }
}
