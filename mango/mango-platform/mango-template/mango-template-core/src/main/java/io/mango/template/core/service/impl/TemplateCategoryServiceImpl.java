package io.mango.template.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.template.api.TemplateCode;
import io.mango.template.api.command.SaveTemplateCategoryCommand;
import io.mango.template.api.command.UpdateTemplateCategoryStatusCommand;
import io.mango.template.api.enums.TemplateStatus;
import io.mango.template.api.query.TemplateCategoryPageQuery;
import io.mango.template.api.vo.TemplateCategoryVO;
import io.mango.template.core.entity.TemplateCategory;
import io.mango.template.core.entity.Template;
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
public class TemplateCategoryServiceImpl implements ITemplateCategoryService {

    private final TemplateCategoryMapper categoryMapper;
    private final TemplateMapper templateMapper;

    @Override
    public R<PageResult<TemplateCategoryVO>> page(TemplateCategoryPageQuery query) {
        TemplateCategoryPageQuery resolved = query == null ? new TemplateCategoryPageQuery() : query;
        IPage<TemplateCategory> page = categoryMapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                wrapper(resolved));
        return R.ok(PageResult.of(page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<List<TemplateCategoryVO>> list(TemplateCategoryPageQuery query) {
        TemplateCategoryPageQuery resolved = query == null ? new TemplateCategoryPageQuery() : query;
        if (resolved.getStatus() == null) {
            resolved.setStatus(TemplateStatus.ENABLED.value());
        }
        return R.ok(categoryMapper.selectList(wrapper(resolved)).stream().map(this::toVO).toList());
    }

    @Override
    public R<TemplateCategoryVO> detail(Long id) {
        return R.ok(toVO(selectCategory(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> create(SaveTemplateCategoryCommand command) {
        validateSave(command, false);
        Long tenantId = requireTenantId();
        Require.isNull(categoryMapper.selectOne(new LambdaQueryWrapper<TemplateCategory>()
                .eq(TemplateCategory::getTenantId, tenantId)
                .eq(TemplateCategory::getCategoryCode, command.getCategoryCode().trim())
                .last("LIMIT 1")), TemplateCode.TEMPLATE_CATEGORY_CODE_DUPLICATED);
        TemplateCategory entity = new TemplateCategory();
        entity.setTenantId(tenantId);
        apply(entity, command);
        entity.setStatus(command.getStatus() == null ? TemplateStatus.ENABLED.value() : command.getStatus());
        entity.setCreatedBy(MangoContextHolder.userId());
        entity.setUpdatedBy(MangoContextHolder.userId());
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedTime(now);
        entity.setUpdatedTime(now);
        categoryMapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> update(SaveTemplateCategoryCommand command) {
        validateSave(command, true);
        TemplateCategory entity = selectCategory(command.getId());
        if (!entity.getCategoryCode().equals(command.getCategoryCode().trim())) {
            Require.isNull(categoryMapper.selectOne(new LambdaQueryWrapper<TemplateCategory>()
                    .eq(TemplateCategory::getTenantId, entity.getTenantId())
                    .eq(TemplateCategory::getCategoryCode, command.getCategoryCode().trim())
                    .last("LIMIT 1")), TemplateCode.TEMPLATE_CATEGORY_CODE_DUPLICATED);
        }
        apply(entity, command);
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedTime(LocalDateTime.now());
        return R.ok(categoryMapper.updateById(entity) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateStatus(UpdateTemplateCategoryStatusCommand command) {
        Require.notNull(command, "模板分类状态命令不能为空");
        TemplateCategory entity = selectCategory(command.getId());
        entity.setStatus(command.getStatus());
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedTime(LocalDateTime.now());
        return R.ok(categoryMapper.updateById(entity) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(Long id) {
        TemplateCategory entity = selectCategory(id);
        Long templateCount = templateMapper.selectCount(new LambdaQueryWrapper<Template>()
                .eq(Template::getTenantId, entity.getTenantId())
                .eq(Template::getCategoryCode, entity.getCategoryCode()));
        Require.isTrue(templateCount == 0, "分类已被模板使用，不能删除");
        return R.ok(categoryMapper.deleteById(entity.getId()) > 0);
    }

    private void validateSave(SaveTemplateCategoryCommand command, boolean update) {
        Require.notNull(command, "模板分类保存命令不能为空");
        if (update) {
            Require.notNull(command.getId(), "模板分类ID不能为空");
        }
        Require.notBlank(command.getCategoryCode(), "分类编码不能为空");
        Require.notBlank(command.getCategoryName(), "分类名称不能为空");
    }

    private void apply(TemplateCategory entity, SaveTemplateCategoryCommand command) {
        entity.setCategoryCode(command.getCategoryCode().trim());
        entity.setCategoryName(command.getCategoryName().trim());
        entity.setSort(command.getSort() == null ? 0 : command.getSort());
        if (command.getStatus() != null) {
            entity.setStatus(command.getStatus());
        }
        entity.setRemark(trimToNull(command.getRemark()));
    }

    private TemplateCategory selectCategory(Long id) {
        Require.notNull(id, "模板分类ID不能为空");
        TemplateCategory entity = categoryMapper.selectById(id);
        Require.notNull(entity, TemplateCode.TEMPLATE_CATEGORY_NOT_FOUND);
        Require.isTrue(entity.getTenantId().equals(requireTenantId()), TemplateCode.TEMPLATE_CATEGORY_NOT_FOUND);
        return entity;
    }

    private LambdaQueryWrapper<TemplateCategory> wrapper(TemplateCategoryPageQuery query) {
        LambdaQueryWrapper<TemplateCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TemplateCategory::getTenantId, requireTenantId());
        String keyword = trimToNull(query.getKeyword());
        wrapper.and(StringUtils.hasText(keyword), item -> item
                .like(TemplateCategory::getCategoryCode, keyword)
                .or()
                .like(TemplateCategory::getCategoryName, keyword));
        wrapper.eq(query.getStatus() != null, TemplateCategory::getStatus, query.getStatus());
        wrapper.orderByDesc(TemplateCategory::getId);
        return wrapper;
    }

    private Long requireTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, "机构上下文不能为空");
        try {
            return Long.parseLong(tenantId);
        } catch (NumberFormatException e) {
            throw new BizException("机构上下文非法");
        }
    }

    private TemplateCategoryVO toVO(TemplateCategory entity) {
        TemplateCategoryVO vo = new TemplateCategoryVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setCategoryName(entity.getCategoryName());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedTime(entity.getCreatedTime());
        vo.setUpdatedTime(entity.getUpdatedTime());
        return vo;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
