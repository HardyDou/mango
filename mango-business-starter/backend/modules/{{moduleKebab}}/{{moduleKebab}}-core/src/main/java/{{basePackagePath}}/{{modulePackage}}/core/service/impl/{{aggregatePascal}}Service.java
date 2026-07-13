package {{basePackage}}.{{modulePackage}}.core.service.impl;

import {{basePackage}}.{{modulePackage}}.api.enums.{{aggregatePascal}}Code;
import {{basePackage}}.{{modulePackage}}.api.command.Create{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.command.Update{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.query.{{aggregatePascal}}PageQuery;
import {{basePackage}}.{{modulePackage}}.api.vo.{{aggregatePascal}}VO;
import {{basePackage}}.{{modulePackage}}.core.entity.{{aggregatePascal}}Entity;
import {{basePackage}}.{{modulePackage}}.core.mapper.{{aggregatePascal}}Mapper;
import {{basePackage}}.{{modulePackage}}.core.service.I{{aggregatePascal}}Service;
import io.mango.common.result.Require;
import io.mango.infra.persistence.api.crud.DeleteCommand;
import io.mango.infra.persistence.api.crud.MangoCrudServiceImpl;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(rollbackFor = Exception.class)
    public Long create(Create{{aggregatePascal}}Command command) {
        Require.notNull(command, {{aggregatePascal}}Code.VALIDATION_ERROR);
        Object id = createByCommand(command);
        Require.isTrue(id instanceof Long, {{aggregatePascal}}Code.VALIDATION_ERROR);
        return (Long) id;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Update{{aggregatePascal}}Command command) {
        Require.notNull(command, {{aggregatePascal}}Code.VALIDATION_ERROR);
        Require.notNull(getById(command.getId()), {{aggregatePascal}}Code.NOT_FOUND);
        return updateByCommand(command);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(DeleteCommand command) {
        Require.notNull(command, {{aggregatePascal}}Code.VALIDATION_ERROR);
        Require.notNull(command.getId(), {{aggregatePascal}}Code.VALIDATION_ERROR);
        Require.notNull(getById(command.getId()), {{aggregatePascal}}Code.NOT_FOUND);
        return deleteById(command.getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public PersistencePageResult<{{aggregatePascal}}VO> page({{aggregatePascal}}PageQuery query) {
        Require.notNull(query, {{aggregatePascal}}Code.VALIDATION_ERROR);
        return (PersistencePageResult<{{aggregatePascal}}VO>) (PersistencePageResult<?>)
                pageByQuery(query);
    }

    @Override
    public {{aggregatePascal}}VO detail(Long id) {
        Require.notNull(id, {{aggregatePascal}}Code.VALIDATION_ERROR);
        {{aggregatePascal}}Entity entity = getById(id);
        Require.notNull(entity, {{aggregatePascal}}Code.NOT_FOUND);
        return toVO(entity);
    }

    @Override
    protected Class<{{aggregatePascal}}Entity> entityType() {
        return {{aggregatePascal}}Entity.class;
    }
}
