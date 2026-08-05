package {{basePackage}}.{{modulePackage}}.core.service;

import {{basePackage}}.{{modulePackage}}.api.command.Create{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.command.Update{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.query.{{aggregatePascal}}PageQuery;
import {{basePackage}}.{{modulePackage}}.api.vo.{{aggregatePascal}}VO;
import {{basePackage}}.{{modulePackage}}.core.entity.{{aggregatePascal}}Entity;
import io.mango.infra.persistence.api.crud.MangoTypedCrudService;

/**
 * {{aggregatePascal}}内部服务。
 */
public interface I{{aggregatePascal}}Service extends MangoTypedCrudService<
        {{aggregatePascal}}Entity,
        Create{{aggregatePascal}}Command,
        Update{{aggregatePascal}}Command,
        {{aggregatePascal}}PageQuery,
        {{aggregatePascal}}VO,
        Long> {
}
