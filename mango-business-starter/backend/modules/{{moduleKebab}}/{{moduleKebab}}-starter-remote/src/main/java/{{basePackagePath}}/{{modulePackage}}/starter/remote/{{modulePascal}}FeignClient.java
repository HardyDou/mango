package {{basePackage}}.{{modulePackage}}.starter.remote;

import {{basePackage}}.{{modulePackage}}.api.{{modulePascal}}Api;
import {{basePackage}}.{{modulePackage}}.api.command.Create{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.command.Update{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.query.{{aggregatePascal}}PageQuery;
import {{basePackage}}.{{modulePackage}}.api.vo.{{aggregatePascal}}VO;
import io.mango.common.result.R;
import io.mango.infra.persistence.api.crud.DeleteCommand;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * {{moduleName}}远程适配器。
 */
@FeignClient(
        name = "{{moduleKebab}}",
        contextId = "{{moduleCamel}}FeignClient",
        path = "/{{moduleKebab}}/{{aggregateKebab}}s")
public interface {{modulePascal}}FeignClient extends {{modulePascal}}Api {

    @Override
    @PostMapping("/create")
    R<Long> create(@RequestBody @Valid Create{{aggregatePascal}}Command command);

    @Override
    @PostMapping("/update")
    R<Boolean> update(@RequestBody @Valid Update{{aggregatePascal}}Command command);

    @Override
    @PostMapping("/delete")
    R<Boolean> delete(@RequestBody @Valid DeleteCommand command);

    @Override
    @GetMapping("/page")
    R<PersistencePageResult<{{aggregatePascal}}VO>> page(
            @SpringQueryMap @Valid {{aggregatePascal}}PageQuery query);

    @Override
    @GetMapping("/detail")
    R<{{aggregatePascal}}VO> detail(@RequestParam("id") @NotNull Long id);
}
