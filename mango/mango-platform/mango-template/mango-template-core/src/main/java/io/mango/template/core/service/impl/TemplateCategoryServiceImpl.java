package io.mango.template.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.persistence.api.crud.DeleteCommand;
import io.mango.infra.persistence.api.crud.MangoCrudServiceImpl;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import io.mango.template.api.enums.TemplateCode;
import io.mango.template.api.command.CreateTemplateCategoryCommand;
import io.mango.template.api.command.SaveTemplateCategoryCommand;
import io.mango.template.api.command.UpdateTemplateCategoryCommand;
import io.mango.template.api.command.UpdateTemplateCategoryStatusCommand;
import io.mango.template.api.enums.TemplateStatus;
import io.mango.template.api.query.TemplateCategoryPageQuery;
import io.mango.template.api.vo.TemplateCategoryVO;
import io.mango.template.core.entity.TemplateCategoryEntity;
import io.mango.template.core.entity.TemplateEntity;
import io.mango.template.core.mapper.TemplateCategoryMapper;
import io.mango.template.core.mapper.TemplateMapper;
import io.mango.template.core.service.ITemplateCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模板分类服务实现。
 */
@Service
@RequiredArgsConstructor
public class TemplateCategoryServiceImpl extends MangoCrudServiceImpl<TemplateCategoryMapper, TemplateCategoryEntity>
        implements ITemplateCategoryService {

    private final TemplateCategoryMapper categoryMapper;
    private final TemplateMapper templateMapper;

    @Override
    public PageResult<TemplateCategoryVO> pageResult(TemplateCategoryPageQuery query) {
        TemplateCategoryPageQuery resolved = query;
        if (resolved == null) {
            resolved = new TemplateCategoryPageQuery();
        }
        IPage<TemplateCategoryEntity> page = categoryMapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                wrapper(resolved));
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PersistencePageResult<TemplateCategoryVO> page(TemplateCategoryPageQuery query) {
        PageResult<TemplateCategoryVO> result = pageResult(query);
        return PersistencePageResult.of(result.getList(), result.getTotal(), result.getPage(), result.getSize());
    }

    @Override
    public List<TemplateCategoryVO> list(TemplateCategoryPageQuery query) {
        TemplateCategoryPageQuery resolved = query;
        if (resolved == null) {
            resolved = new TemplateCategoryPageQuery();
        }
        if (resolved.getStatus() == null) {
            resolved.setStatus(TemplateStatus.ENABLED.value());
        }
        return categoryMapper.selectList(wrapper(resolved)).stream().map(this::toVO).toList();
    }

    @Override
    public TemplateCategoryVO detail(Long id) {
        return toVO(selectCategory(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CreateTemplateCategoryCommand command) {
        validateSave(command);
        String tenantId = requireTenantId();
        Require.isNull(categoryMapper.selectOne(new LambdaQueryWrapper<TemplateCategoryEntity>()
                .eq(TemplateCategoryEntity::getTenantId, tenantId)
                .eq(TemplateCategoryEntity::getCategoryCode, command.getCategoryCode().trim())
                .last("LIMIT 1")), TemplateCode.TEMPLATE_CATEGORY_CODE_DUPLICATED);
        TemplateCategoryEntity entity = new TemplateCategoryEntity();
        entity.setTenantId(tenantId);
        apply(entity, command);
        if (command.getStatus() == null) {
            entity.setStatus(TemplateStatus.ENABLED.value());
        } else {
            entity.setStatus(command.getStatus());
        }
        entity.setCreatedBy(MangoContextHolder.userId());
        entity.setUpdatedBy(MangoContextHolder.userId());
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        categoryMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(UpdateTemplateCategoryCommand command) {
        validateSave(command);
        Require.notNull(command.getId(), TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板分类ID不能为空");
        TemplateCategoryEntity entity = selectCategory(command.getId());
        if (!entity.getCategoryCode().equals(command.getCategoryCode().trim())) {
            Require.isNull(categoryMapper.selectOne(new LambdaQueryWrapper<TemplateCategoryEntity>()
                    .eq(TemplateCategoryEntity::getTenantId, entity.getTenantId())
                    .eq(TemplateCategoryEntity::getCategoryCode, command.getCategoryCode().trim())
                    .last("LIMIT 1")), TemplateCode.TEMPLATE_CATEGORY_CODE_DUPLICATED);
        }
        apply(entity, command);
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedAt(LocalDateTime.now());
        return categoryMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(UpdateTemplateCategoryStatusCommand command) {
        Require.notNull(command, TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板分类状态命令不能为空");
        TemplateCategoryEntity entity = selectCategory(command.getId());
        entity.setStatus(command.getStatus());
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedAt(LocalDateTime.now());
        return categoryMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        TemplateCategoryEntity entity = selectCategory(id);
        Long templateCount = templateMapper.selectCount(new LambdaQueryWrapper<TemplateEntity>()
                .eq(TemplateEntity::getTenantId, entity.getTenantId())
                .eq(TemplateEntity::getCategoryCode, entity.getCategoryCode()));
        Require.isTrue(templateCount == 0, TemplateCode.TEMPLATE_VALIDATION_ERROR, "分类已被模板使用，不能删除");
        return categoryMapper.deleteById(entity.getId()) > 0;
    }

    @Override
    public boolean delete(DeleteCommand command) {
        Require.notNull(command, TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板分类删除命令不能为空");
        Require.notNull(command.getId(), TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板分类ID不能为空");
        return delete(Long.valueOf(String.valueOf(command.getId())));
    }

    private void validateSave(SaveTemplateCategoryCommand command) {
        Require.notNull(command, TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板分类保存命令不能为空");
        Require.notBlank(command.getCategoryCode(), TemplateCode.TEMPLATE_VALIDATION_ERROR, "分类编码不能为空");
        Require.notBlank(command.getCategoryName(), TemplateCode.TEMPLATE_VALIDATION_ERROR, "分类名称不能为空");
    }

    private void apply(TemplateCategoryEntity entity, SaveTemplateCategoryCommand command) {
        entity.setCategoryCode(command.getCategoryCode().trim());
        entity.setCategoryName(command.getCategoryName().trim());
        if (command.getSort() == null) {
            entity.setSort(0);
        } else {
            entity.setSort(command.getSort());
        }
        if (command.getStatus() != null) {
            entity.setStatus(command.getStatus());
        }
        entity.setRemark(trimToNull(command.getRemark()));
    }

    private TemplateCategoryEntity selectCategory(Long id) {
        Require.notNull(id, TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板分类ID不能为空");
        TemplateCategoryEntity entity = categoryMapper.selectById(id);
        Require.notNull(entity, TemplateCode.TEMPLATE_CATEGORY_NOT_FOUND);
        Require.isTrue(entity.getTenantId().equals(requireTenantId()), TemplateCode.TEMPLATE_CATEGORY_NOT_FOUND);
        return entity;
    }

    private LambdaQueryWrapper<TemplateCategoryEntity> wrapper(TemplateCategoryPageQuery query) {
        LambdaQueryWrapper<TemplateCategoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TemplateCategoryEntity::getTenantId, requireTenantId());
        String keyword = trimToNull(query.getKeyword());
        wrapper.and(StringUtils.hasText(keyword), item -> item
                .like(TemplateCategoryEntity::getCategoryCode, keyword)
                .or()
                .like(TemplateCategoryEntity::getCategoryName, keyword));
        wrapper.eq(query.getStatus() != null, TemplateCategoryEntity::getStatus, query.getStatus());
        wrapper.orderByDesc(TemplateCategoryEntity::getId);
        return wrapper;
    }

    private String requireTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, TemplateCode.TEMPLATE_VALIDATION_ERROR, "机构上下文不能为空");
        return tenantId;
    }

    @Override
    protected TemplateCategoryVO toVO(TemplateCategoryEntity entity) {
        TemplateCategoryVO vo = new TemplateCategoryVO();
        vo.setId(entity.getId());
        vo.setTenantId(Long.valueOf(entity.getTenantId()));
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setCategoryName(entity.getCategoryName());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedTime(entity.getCreatedAt());
        vo.setUpdatedTime(entity.getUpdatedAt());
        return vo;
    }

    @Override
    protected Class<TemplateCategoryEntity> entityType() {
        return TemplateCategoryEntity.class;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
