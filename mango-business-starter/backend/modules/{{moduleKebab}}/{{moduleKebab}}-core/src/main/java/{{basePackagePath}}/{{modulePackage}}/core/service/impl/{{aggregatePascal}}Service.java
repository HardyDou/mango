package {{basePackage}}.{{modulePackage}}.core.service.impl;

import {{basePackage}}.{{modulePackage}}.api.vo.{{aggregatePascal}}VO;
import {{basePackage}}.{{modulePackage}}.core.entity.{{aggregatePascal}}Entity;
import {{basePackage}}.{{modulePackage}}.core.mapper.{{aggregatePascal}}Mapper;
import {{basePackage}}.{{modulePackage}}.core.service.I{{aggregatePascal}}Service;
import io.mango.infra.persistence.starter.crud.MangoCrudServiceImpl;
import org.springframework.stereotype.Service;

/**
 * {{aggregatePascal}}服务实现。
 */
@Service
public class {{aggregatePascal}}Service
        extends MangoCrudServiceImpl<{{aggregatePascal}}Mapper, {{aggregatePascal}}Entity>
        implements I{{aggregatePascal}}Service {

    @Override
    protected {{aggregatePascal}}VO toVO({{aggregatePascal}}Entity entity) {
        if (entity == null) {
            return null;
        }
        {{aggregatePascal}}VO vo = new {{aggregatePascal}}VO();
        vo.setId(String.valueOf(entity.getId()));
        vo.setName(entity.getName());
        return vo;
    }

    @Override
    protected Class<{{aggregatePascal}}Entity> entityType() {
        return {{aggregatePascal}}Entity.class;
    }
}
