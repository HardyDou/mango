package {{basePackage}}.{{modulePackage}}.api;

import {{basePackage}}.{{modulePackage}}.api.command.Create{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.command.Update{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.query.{{aggregatePascal}}PageQuery;
import io.mango.common.result.R;
import io.mango.infra.persistence.api.crud.DeleteCommand;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * {{moduleName}}对外接口。
 */
@Validated
@Tag(name = "{{moduleName}}", description = "{{moduleName}}接口")
public interface {{modulePascal}}Api {

    @Operation(summary = "创建{{aggregateName}}", description = "创建{{aggregateName}}业务记录")
    @PostMapping("/{{aggregateKebab}}s/create")
    R<Object> create(@Valid @RequestBody Create{{aggregatePascal}}Command command);

    @Operation(summary = "修改{{aggregateName}}", description = "修改{{aggregateName}}业务记录")
    @PostMapping("/{{aggregateKebab}}s/update")
    R<Boolean> update(@Valid @RequestBody Update{{aggregatePascal}}Command command);

    @Operation(summary = "删除{{aggregateName}}", description = "按业务标识删除{{aggregateName}}业务记录")
    @PostMapping("/{{aggregateKebab}}s/delete")
    R<Boolean> delete(@Valid @RequestBody DeleteCommand command);

    @Operation(summary = "分页查询{{aggregateName}}", description = "按查询条件分页获取{{aggregateName}}")
    @GetMapping("/{{aggregateKebab}}s/page")
    R<PersistencePageResult<?>> page(@Valid @ParameterObject {{aggregatePascal}}PageQuery query);

    @Operation(summary = "查询{{aggregateName}}详情", description = "按业务标识获取{{aggregateName}}详情")
    @GetMapping("/{{aggregateKebab}}s/detail")
    R<Object> detail(@RequestParam("id") Long id);
}
