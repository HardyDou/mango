package {{basePackage}}.{{modulePackage}}.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import {{basePackage}}.{{modulePackage}}.api.command.Create{{aggregatePascal}}Command;
import {{basePackage}}.{{modulePackage}}.api.query.{{aggregatePascal}}PageQuery;
import {{basePackage}}.{{modulePackage}}.api.vo.{{aggregatePascal}}VO;
import {{basePackage}}.{{modulePackage}}.core.entity.{{aggregatePascal}}Entity;
import {{basePackage}}.{{modulePackage}}.core.mapper.{{aggregatePascal}}Mapper;
import {{basePackage}}.{{modulePackage}}.core.service.I{{aggregatePascal}}Service;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * {{aggregatePascal}}服务实现。
 */
@Service
public class {{aggregatePascal}}Service implements I{{aggregatePascal}}Service {

    private final {{aggregatePascal}}Mapper {{aggregateCamel}}Mapper;

    public {{aggregatePascal}}Service({{aggregatePascal}}Mapper {{aggregateCamel}}Mapper) {
        this.{{aggregateCamel}}Mapper = {{aggregateCamel}}Mapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public {{aggregatePascal}}VO create(Create{{aggregatePascal}}Command command) {
        Require.notNull(command, "{{aggregatePascal}}创建命令不能为空");
        Require.notBlank(command.getName(), "{{aggregatePascal}}名称不能为空");
        {{aggregatePascal}}Entity entity = new {{aggregatePascal}}Entity();
        entity.setName(command.getName());
        {{aggregateCamel}}Mapper.insert(entity);
        return toVO(entity);
    }

    @Override
    public PageResult<{{aggregatePascal}}VO> page({{aggregatePascal}}PageQuery query) {
        {{aggregatePascal}}PageQuery resolved = query == null ? new {{aggregatePascal}}PageQuery() : query;
        LambdaQueryWrapper<{{aggregatePascal}}Entity> wrapper = new LambdaQueryWrapper<{{aggregatePascal}}Entity>()
                .like(StringUtils.hasText(resolved.getName()), {{aggregatePascal}}Entity::getName, resolved.getName())
                .orderByDesc({{aggregatePascal}}Entity::getCreatedAt);
        IPage<{{aggregatePascal}}Entity> page = {{aggregateCamel}}Mapper.selectPage(
                new Page<>(resolved.getPageNo(), resolved.getPageSize()), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public {{aggregatePascal}}VO detail(String id) {
        Require.notBlank(id, "{{aggregatePascal}}业务标识不能为空");
        {{aggregatePascal}}Entity entity = {{aggregateCamel}}Mapper.selectById(id);
        Require.notNull(entity, 404, "{{aggregatePascal}}记录不存在");
        return toVO(entity);
    }

    private {{aggregatePascal}}VO toVO({{aggregatePascal}}Entity entity) {
        if (entity == null) {
            return null;
        }
        {{aggregatePascal}}VO vo = new {{aggregatePascal}}VO();
        vo.setId(String.valueOf(entity.getId()));
        vo.setName(entity.getName());
        return vo;
    }
}
