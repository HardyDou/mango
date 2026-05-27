package {{basePackage}}.{{modulePackage}}.starter.controller;

import {{basePackage}}.{{modulePackage}}.api.{{modulePascal}}Api;
import {{basePackage}}.{{modulePackage}}.api.command.Create{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.query.{{aggregatePascal}}PageQuery;
import {{basePackage}}.{{modulePackage}}.api.vo.{{aggregatePascal}}VO;
import {{basePackage}}.{{modulePackage}}.core.service.I{{aggregatePascal}}Service;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {{moduleName}}接口适配器。
 */
@RestController
@RequestMapping("/{{moduleKebab}}")
public class {{modulePascal}}Controller implements {{modulePascal}}Api {

    private final I{{aggregatePascal}}Service {{aggregateCamel}}Service;

    public {{modulePascal}}Controller(I{{aggregatePascal}}Service {{aggregateCamel}}Service) {
        this.{{aggregateCamel}}Service = {{aggregateCamel}}Service;
    }

    @Override
    public R<{{aggregatePascal}}VO> create(Create{{aggregatePascal}}Command command) {
        return R.ok({{aggregateCamel}}Service.create(command));
    }

    @Override
    public R<PageResult<{{aggregatePascal}}VO>> page({{aggregatePascal}}PageQuery query) {
        return R.ok({{aggregateCamel}}Service.page(query));
    }

    @Override
    public R<{{aggregatePascal}}VO> detail(String id) {
        return R.ok({{aggregateCamel}}Service.detail(id));
    }
}
