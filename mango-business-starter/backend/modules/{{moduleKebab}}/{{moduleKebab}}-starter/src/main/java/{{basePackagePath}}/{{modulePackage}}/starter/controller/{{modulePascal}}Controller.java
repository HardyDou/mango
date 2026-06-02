package {{basePackage}}.{{modulePackage}}.starter.controller;

import {{basePackage}}.{{modulePackage}}.api.command.Create{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.command.Update{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.query.{{aggregatePascal}}PageQuery;
import {{basePackage}}.{{modulePackage}}.core.service.I{{aggregatePascal}}Service;
import io.mango.infra.persistence.web.starter.controller.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {{moduleName}}接口适配器。
 */
@RestController
@RequestMapping("/{{moduleKebab}}/{{aggregateKebab}}s")
public class {{modulePascal}}Controller
        extends BaseCrudController<I{{aggregatePascal}}Service, Create{{aggregatePascal}}Command,
        Update{{aggregatePascal}}Command, {{aggregatePascal}}PageQuery> {

    public {{modulePascal}}Controller(I{{aggregatePascal}}Service {{aggregateCamel}}Service) {
        super({{aggregateCamel}}Service);
    }

    @Override
    protected Class<{{aggregatePascal}}PageQuery> queryType() {
        return {{aggregatePascal}}PageQuery.class;
    }
}
