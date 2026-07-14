package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.workflow.api.enums.WorkflowCode;
import io.mango.workflow.api.command.SaveWorkflowCategoryCommand;
import io.mango.workflow.api.query.WorkflowCategoryPageQuery;
import io.mango.workflow.api.vo.WorkflowCategoryVO;
import io.mango.workflow.core.entity.WorkflowCategoryEntity;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowCategoryMapper;
import io.mango.workflow.core.service.IWorkflowCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程分类服务实现。
 */
@Service
@RequiredArgsConstructor
public class WorkflowCategoryService implements IWorkflowCategoryService {

    private final WorkflowCategoryMapper mapper;
    private final WorkflowDefinitionMapper definitionMapper;

    @Override
    public PageResult<WorkflowCategoryVO> page(WorkflowCategoryPageQuery query) {
        WorkflowCategoryPageQuery resolved = query == null ? new WorkflowCategoryPageQuery() : query;
        IPage<WorkflowCategoryEntity> page = mapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                wrapper(resolved));
        List<WorkflowCategoryVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<WorkflowCategoryVO> list(Integer status, String domainCode) {
        LambdaQueryWrapper<WorkflowCategoryEntity> wrapper = new LambdaQueryWrapper<WorkflowCategoryEntity>()
                .eq(status != null, WorkflowCategoryEntity::getStatus, status)
                .eq(StringUtils.hasText(domainCode), WorkflowCategoryEntity::getDomainCode, trimToNull(domainCode))
                .orderByAsc(WorkflowCategoryEntity::getSort)
                .orderByDesc(WorkflowCategoryEntity::getUpdatedTime);
        return mapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public WorkflowCategoryVO get(Long id) {
        return toVO(selectRequired(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(SaveWorkflowCategoryCommand command) {
        Require.notNull(command, WorkflowCode.CATEGORY_INVALID);
        validate(command);
        WorkflowCategoryEntity entity = new WorkflowCategoryEntity();
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
    public Boolean update(SaveWorkflowCategoryCommand command) {
        Require.notNull(command, WorkflowCode.CATEGORY_INVALID);
        Require.notNull(command.getId(), WorkflowCode.CATEGORY_INVALID, "流程分类ID不能为空");
        validate(command);
        WorkflowCategoryEntity entity = selectRequired(command.getId());
        copy(command, entity);
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedTime(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return mapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long id) {
        WorkflowCategoryEntity entity = selectRequired(id);
        Long count = definitionMapper.selectCount(new LambdaQueryWrapper<io.mango.workflow.core.entity.WorkflowDefinitionEntity>()
                .eq(io.mango.workflow.core.entity.WorkflowDefinitionEntity::getCategoryId, entity.getId()));
        Require.isTrue(count == null || count == 0, WorkflowCode.CATEGORY_INVALID, "流程分类下存在流程定义，不能删除");
        return mapper.deleteById(id) > 0;
    }

    private LambdaQueryWrapper<WorkflowCategoryEntity> wrapper(WorkflowCategoryPageQuery query) {
        String keyword = trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<WorkflowCategoryEntity>()
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(WorkflowCategoryEntity::getCategoryName, keyword)
                        .or()
                        .like(WorkflowCategoryEntity::getCategoryCode, keyword))
                .eq(query.getStatus() != null, WorkflowCategoryEntity::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getDomainCode()), WorkflowCategoryEntity::getDomainCode, trimToNull(query.getDomainCode()))
                .orderByAsc(WorkflowCategoryEntity::getSort)
                .orderByDesc(WorkflowCategoryEntity::getUpdatedTime);
    }

    private WorkflowCategoryEntity selectRequired(Long id) {
        Require.notNull(id, WorkflowCode.CATEGORY_INVALID, "流程分类ID不能为空");
        WorkflowCategoryEntity entity = mapper.selectById(id);
        Require.notNull(entity, WorkflowCode.CATEGORY_NOT_FOUND);
        return entity;
    }

    private void validate(SaveWorkflowCategoryCommand command) {
        Require.notBlank(command.getCategoryName(), WorkflowCode.CATEGORY_INVALID, "分类名称不能为空");
        Require.notBlank(command.getCategoryCode(), WorkflowCode.CATEGORY_INVALID, "分类编码不能为空");
    }

    private void copy(SaveWorkflowCategoryCommand command, WorkflowCategoryEntity entity) {
        entity.setCategoryName(command.getCategoryName().trim());
        entity.setCategoryCode(command.getCategoryCode().trim());
        entity.setDomainCode(resolveDomainCode(command.getDomainCode()));
        entity.setSort(command.getSort() == null ? 0 : command.getSort());
        entity.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        entity.setRemark(trimToNull(command.getRemark()));
    }

    private WorkflowCategoryVO toVO(WorkflowCategoryEntity entity) {
        WorkflowCategoryVO vo = new WorkflowCategoryVO();
        vo.setId(entity.getId());
        vo.setCategoryName(entity.getCategoryName());
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setDomainCode(entity.getDomainCode());
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

    private String resolveDomainCode(String domainCode) {
        return StringUtils.hasText(domainCode) ? domainCode.trim() : "COMMON";
    }
}
