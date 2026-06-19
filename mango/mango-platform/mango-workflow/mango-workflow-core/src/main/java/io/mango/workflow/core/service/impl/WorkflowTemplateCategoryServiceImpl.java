package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.workflow.api.WorkflowCode;
import io.mango.workflow.api.command.SaveWorkflowTemplateCategoryCommand;
import io.mango.workflow.api.query.WorkflowTemplateCategoryPageQuery;
import io.mango.workflow.api.vo.WorkflowTemplateCategoryVO;
import io.mango.workflow.core.entity.WorkflowTemplate;
import io.mango.workflow.core.entity.WorkflowTemplateCategory;
import io.mango.workflow.core.mapper.WorkflowTemplateCategoryMapper;
import io.mango.workflow.core.mapper.WorkflowTemplateMapper;
import io.mango.workflow.core.service.IWorkflowTemplateCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程模板分类服务实现。
 */
@Service
@RequiredArgsConstructor
public class WorkflowTemplateCategoryServiceImpl implements IWorkflowTemplateCategoryService {

    private final WorkflowTemplateCategoryMapper mapper;
    private final WorkflowTemplateMapper templateMapper;

    @Override
    public R<PageResult<WorkflowTemplateCategoryVO>> page(WorkflowTemplateCategoryPageQuery query) {
        WorkflowTemplateCategoryPageQuery resolved = query == null ? new WorkflowTemplateCategoryPageQuery() : query;
        IPage<WorkflowTemplateCategory> page = mapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                wrapper(resolved));
        List<WorkflowTemplateCategoryVO> records = page.getRecords().stream().map(this::toVO).toList();
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<List<WorkflowTemplateCategoryVO>> list(Integer status) {
        LambdaQueryWrapper<WorkflowTemplateCategory> wrapper = new LambdaQueryWrapper<WorkflowTemplateCategory>()
                .eq(status != null, WorkflowTemplateCategory::getStatus, status)
                .orderByAsc(WorkflowTemplateCategory::getSort)
                .orderByDesc(WorkflowTemplateCategory::getUpdatedTime);
        return R.ok(mapper.selectList(wrapper).stream().map(this::toVO).toList());
    }

    @Override
    public R<WorkflowTemplateCategoryVO> get(Long id) {
        return R.ok(toVO(selectRequired(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> create(SaveWorkflowTemplateCategoryCommand command) {
        Require.notNull(command, WorkflowCode.TEMPLATE_CATEGORY_INVALID);
        validate(command);
        WorkflowTemplateCategory entity = new WorkflowTemplateCategory();
        copy(command, entity);
        LocalDateTime now = LocalDateTime.now();
        entity.setTenantId(resolveTenantId());
        entity.setCreatedBy(MangoContextHolder.userId());
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setCreatedTime(now);
        entity.setCreatedAt(now);
        entity.setUpdatedTime(now);
        entity.setUpdatedAt(now);
        mapper.insert(entity);
        return R.ok(String.valueOf(entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> update(SaveWorkflowTemplateCategoryCommand command) {
        Require.notNull(command, WorkflowCode.TEMPLATE_CATEGORY_INVALID);
        Require.notNull(command.getId(), WorkflowCode.TEMPLATE_CATEGORY_INVALID.getCode(), "流程模板分类ID不能为空");
        validate(command);
        WorkflowTemplateCategory entity = selectRequired(command.getId());
        copy(command, entity);
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedTime(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return R.ok(mapper.updateById(entity) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(Long id) {
        WorkflowTemplateCategory entity = selectRequired(id);
        Long childCount = mapper.selectCount(new LambdaQueryWrapper<WorkflowTemplateCategory>()
                .eq(WorkflowTemplateCategory::getParentId, entity.getId()));
        Require.isTrue(childCount == null || childCount == 0, WorkflowCode.TEMPLATE_CATEGORY_INVALID.getCode(), "流程模板分类下存在子分类，不能删除");
        Long templateCount = templateMapper.selectCount(new LambdaQueryWrapper<WorkflowTemplate>()
                .eq(WorkflowTemplate::getTemplateCategoryId, entity.getId()));
        Require.isTrue(templateCount == null || templateCount == 0, WorkflowCode.TEMPLATE_CATEGORY_INVALID.getCode(), "流程模板分类下存在模板，不能删除");
        return R.ok(mapper.deleteById(id) > 0);
    }

    private LambdaQueryWrapper<WorkflowTemplateCategory> wrapper(WorkflowTemplateCategoryPageQuery query) {
        String keyword = trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<WorkflowTemplateCategory>()
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(WorkflowTemplateCategory::getCategoryName, keyword)
                        .or()
                        .like(WorkflowTemplateCategory::getCategoryCode, keyword))
                .eq(query.getStatus() != null, WorkflowTemplateCategory::getStatus, query.getStatus())
                .orderByAsc(WorkflowTemplateCategory::getSort)
                .orderByDesc(WorkflowTemplateCategory::getUpdatedTime);
    }

    private WorkflowTemplateCategory selectRequired(Long id) {
        Require.notNull(id, WorkflowCode.TEMPLATE_CATEGORY_INVALID.getCode(), "流程模板分类ID不能为空");
        WorkflowTemplateCategory entity = mapper.selectById(id);
        Require.notNull(entity, WorkflowCode.TEMPLATE_CATEGORY_NOT_FOUND);
        return entity;
    }

    private void validate(SaveWorkflowTemplateCategoryCommand command) {
        Require.notBlank(command.getCategoryName(), WorkflowCode.TEMPLATE_CATEGORY_INVALID.getCode(), "分类名称不能为空");
        Require.notBlank(command.getCategoryCode(), WorkflowCode.TEMPLATE_CATEGORY_INVALID.getCode(), "分类编码不能为空");
        if (command.getParentId() != null) {
            Require.notNull(mapper.selectById(command.getParentId()), WorkflowCode.TEMPLATE_CATEGORY_NOT_FOUND);
            if (command.getId() != null) {
                Require.isFalse(command.getId().equals(command.getParentId()), WorkflowCode.TEMPLATE_CATEGORY_INVALID.getCode(), "父级分类不能是自身");
            }
        }
    }

    private void copy(SaveWorkflowTemplateCategoryCommand command, WorkflowTemplateCategory entity) {
        entity.setParentId(command.getParentId());
        entity.setCategoryName(command.getCategoryName().trim());
        entity.setCategoryCode(command.getCategoryCode().trim());
        entity.setIcon(trimToNull(command.getIcon()));
        entity.setSort(command.getSort() == null ? 0 : command.getSort());
        entity.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        entity.setRemark(trimToNull(command.getRemark()));
    }

    private WorkflowTemplateCategoryVO toVO(WorkflowTemplateCategory entity) {
        WorkflowTemplateCategoryVO vo = new WorkflowTemplateCategoryVO();
        vo.setId(entity.getId());
        vo.setParentId(entity.getParentId());
        vo.setCategoryName(entity.getCategoryName());
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setIcon(entity.getIcon());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedTime(entity.getCreatedTime());
        vo.setUpdatedTime(entity.getUpdatedTime());
        return vo;
    }

    private Long resolveTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        if (!StringUtils.hasText(tenantId)) {
            return 1L;
        }
        return Long.parseLong(tenantId);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
