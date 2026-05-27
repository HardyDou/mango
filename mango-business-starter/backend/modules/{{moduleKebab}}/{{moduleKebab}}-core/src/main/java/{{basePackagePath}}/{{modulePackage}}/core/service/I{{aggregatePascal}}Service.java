package {{basePackage}}.{{modulePackage}}.core.service;

import {{basePackage}}.{{modulePackage}}.api.command.Create{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.query.{{aggregatePascal}}PageQuery;
import {{basePackage}}.{{modulePackage}}.api.vo.{{aggregatePascal}}VO;
import io.mango.common.vo.PageResult;

/**
 * {{aggregatePascal}}内部服务。
 */
public interface I{{aggregatePascal}}Service {

    {{aggregatePascal}}VO create(Create{{aggregatePascal}}Command command);

    PageResult<{{aggregatePascal}}VO> page({{aggregatePascal}}PageQuery query);

    {{aggregatePascal}}VO detail(String id);
}
