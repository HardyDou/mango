package {{basePackage}}.{{modulePackage}}.core.service.impl;

import {{basePackage}}.{{modulePackage}}.api.command.Create{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.query.{{aggregatePascal}}PageQuery;
import {{basePackage}}.{{modulePackage}}.api.vo.{{aggregatePascal}}VO;
import {{basePackage}}.{{modulePackage}}.core.service.I{{aggregatePascal}}Service;
import io.mango.common.vo.PageResult;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * {{aggregatePascal}}服务实现。
 */
@Service
public class {{aggregatePascal}}Service implements I{{aggregatePascal}}Service {

    @Override
    public {{aggregatePascal}}VO create(Create{{aggregatePascal}}Command command) {
        {{aggregatePascal}}VO vo = new {{aggregatePascal}}VO();
        vo.setId("replace-with-generated-id");
        vo.setName(command.getName());
        return vo;
    }

    @Override
    public PageResult<{{aggregatePascal}}VO> page({{aggregatePascal}}PageQuery query) {
        return PageResult.of(List.of(), 0L, query.getPageNo(), query.getPageSize());
    }

    @Override
    public {{aggregatePascal}}VO detail(String id) {
        {{aggregatePascal}}VO vo = new {{aggregatePascal}}VO();
        vo.setId(id);
        return vo;
    }
}
