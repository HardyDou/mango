package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.workflow.api.enums.WorkflowCode;
import io.mango.workflow.api.command.SaveWorkflowTemplateCategoryCommand;
import io.mango.workflow.api.query.WorkflowTemplateCategoryPageQuery;
import io.mango.workflow.api.vo.WorkflowTemplateCategoryVO;
import io.mango.workflow.core.entity.WorkflowTemplateEntity;
import io.mango.workflow.core.entity.WorkflowTemplateCategoryEntity;
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
public class WorkflowTemplateCategoryService implements IWorkflowTemplateCategoryService {

    private final WorkflowTemplateCategoryMapper mapper;
    private final WorkflowTemplateMapper templateMapper;

    @Override
    public PageResult<WorkflowTemplateCategoryVO> page(WorkflowTemplateCategoryPageQuery query) {
        WorkflowTemplateCategoryPageQuery resolved = query == null ? new WorkflowTemplateCategoryPageQuery() : query;
        IPage<WorkflowTemplateCategoryEntity> page = mapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                wrapper(resolved));
        List<WorkflowTemplateCategoryVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<WorkflowTemplateCategoryVO> list(Integer status) {
        LambdaQueryWrapper<WorkflowTemplateCategoryEntity> wrapper = new LambdaQueryWrapper<WorkflowTemplateCategoryEntity>()
                .eq(status != null, WorkflowTemplateCategoryEntity::getStatus, status)
                .orderByAsc(WorkflowTemplateCategoryEntity::getSort)
                .orderByDesc(WorkflowTemplateCategoryEntity::getUpdatedTime);
        return mapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public WorkflowTemplateCategoryVO get(Long id) {
        return toVO(selectRequired(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(SaveWorkflowTemplateCategoryCommand command) {
        Require.notNull(command, WorkflowCode.TEMPLATE_CATEGORY_INVALID);
        validate(command);
        WorkflowTemplateCategoryEntity entity = new WorkflowTemplateCategoryEntity();
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
        return String.valueOf(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(SaveWorkflowTemplateCategoryCommand command) {
        Require.notNull(command, WorkflowCode.TEMPLATE_CATEGORY_INVALID);
        Require.notNull(command.getId(), WorkflowCode.TEMPLATE_CATEGORY_INVALID, "流程模板分类ID不能为空");
        validate(command);
        WorkflowTemplateCategoryEntity entity = selectRequired(command.getId());
        copy(command, entity);
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedTime(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return mapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long id) {
        WorkflowTemplateCategoryEntity entity = selectRequired(id);
        Long childCount = mapper.selectCount(new LambdaQueryWrapper<WorkflowTemplateCategoryEntity>()
                .eq(WorkflowTemplateCategoryEntity::getParentId, entity.getId()));
        Require.isTrue(childCount == null || childCount == 0, WorkflowCode.TEMPLATE_CATEGORY_INVALID, "流程模板分类下存在子分类，不能删除");
        Long templateCount = templateMapper.selectCount(new LambdaQueryWrapper<WorkflowTemplateEntity>()
                .eq(WorkflowTemplateEntity::getTemplateCategoryId, entity.getId()));
        Require.isTrue(templateCount == null || templateCount == 0, WorkflowCode.TEMPLATE_CATEGORY_INVALID, "流程模板分类下存在模板，不能删除");
        return mapper.deleteById(id) > 0;
    }

    private LambdaQueryWrapper<WorkflowTemplateCategoryEntity> wrapper(WorkflowTemplateCategoryPageQuery query) {
        String keyword = trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<WorkflowTemplateCategoryEntity>()
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(WorkflowTemplateCategoryEntity::getCategoryName, keyword)
                        .or()
                        .like(WorkflowTemplateCategoryEntity::getCategoryCode, keyword))
                .eq(query.getStatus() != null, WorkflowTemplateCategoryEntity::getStatus, query.getStatus())
                .orderByAsc(WorkflowTemplateCategoryEntity::getSort)
                .orderByDesc(WorkflowTemplateCategoryEntity::getUpdatedTime);
    }

    private WorkflowTemplateCategoryEntity selectRequired(Long id) {
        Require.notNull(id, WorkflowCode.TEMPLATE_CATEGORY_INVALID, "流程模板分类ID不能为空");
        WorkflowTemplateCategoryEntity entity = mapper.selectById(id);
        Require.notNull(entity, WorkflowCode.TEMPLATE_CATEGORY_NOT_FOUND);
        return entity;
    }

    private void validate(SaveWorkflowTemplateCategoryCommand command) {
        Require.notBlank(command.getCategoryName(), WorkflowCode.TEMPLATE_CATEGORY_INVALID, "分类名称不能为空");
        Require.notBlank(command.getCategoryCode(), WorkflowCode.TEMPLATE_CATEGORY_INVALID, "分类编码不能为空");
        if (command.getParentId() != null) {
            Require.notNull(mapper.selectById(command.getParentId()), WorkflowCode.TEMPLATE_CATEGORY_NOT_FOUND);
            if (command.getId() != null) {
                Require.isTrue(!command.getId().equals(command.getParentId()),
                        WorkflowCode.TEMPLATE_CATEGORY_INVALID, "父级分类不能是自身");
            }
        }
    }

    private void copy(SaveWorkflowTemplateCategoryCommand command, WorkflowTemplateCategoryEntity entity) {
        entity.setParentId(command.getParentId());
        entity.setCategoryName(command.getCategoryName().trim());
        entity.setCategoryCode(command.getCategoryCode().trim());
        entity.setIcon(trimToNull(command.getIcon()));
        entity.setSort(command.getSort() == null ? 0 : command.getSort());
        entity.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        entity.setRemark(trimToNull(command.getRemark()));
    }

    private WorkflowTemplateCategoryVO toVO(WorkflowTemplateCategoryEntity entity) {
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
