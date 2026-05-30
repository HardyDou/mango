package {{basePackage}}.{{modulePackage}}.api;

import {{basePackage}}.{{modulePackage}}.api.command.Create{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.query.{{aggregatePascal}}PageQuery;
import {{basePackage}}.{{modulePackage}}.api.vo.{{aggregatePascal}}VO;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

    @Operation(summary = "创建{{aggregatePascal}}", description = "创建{{aggregatePascal}}业务记录")
    @PostMapping("/{{aggregateKebab}}s")
    R<{{aggregatePascal}}VO> create(@Valid @RequestBody Create{{aggregatePascal}}Command command);

    @Operation(summary = "分页查询{{aggregatePascal}}", description = "按查询条件分页获取{{aggregatePascal}}")
    @GetMapping("/{{aggregateKebab}}s")
    R<PageResult<{{aggregatePascal}}VO>> page(@Valid @ParameterObject {{aggregatePascal}}PageQuery query);

    @Operation(summary = "查询{{aggregatePascal}}详情", description = "按业务标识获取{{aggregatePascal}}详情")
    @GetMapping("/{{aggregateKebab}}s/detail")
    R<{{aggregatePascal}}VO> detail(@RequestParam("id") @NotBlank(message = "业务标识不能为空") String id);
}
