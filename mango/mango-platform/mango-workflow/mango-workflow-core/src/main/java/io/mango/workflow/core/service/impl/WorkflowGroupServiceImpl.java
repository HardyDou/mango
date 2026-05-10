package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.workflow.api.WorkflowCode;
import io.mango.workflow.api.command.SaveWorkflowGroupCommand;
import io.mango.workflow.api.query.WorkflowGroupPageQuery;
import io.mango.workflow.api.vo.WorkflowGroupVO;
import io.mango.workflow.core.entity.WorkflowGroup;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowGroupMapper;
import io.mango.workflow.core.service.IWorkflowGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程分组服务实现。
 */
@Service
@RequiredArgsConstructor
public class WorkflowGroupServiceImpl implements IWorkflowGroupService {

    private final WorkflowGroupMapper mapper;
    private final WorkflowDefinitionMapper definitionMapper;

    @Override
    public R<PageResult<WorkflowGroupVO>> page(WorkflowGroupPageQuery query) {
        WorkflowGroupPageQuery resolved = query == null ? new WorkflowGroupPageQuery() : query;
        IPage<WorkflowGroup> page = mapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                wrapper(resolved));
        List<WorkflowGroupVO> records = page.getRecords().stream().map(this::toVO).toList();
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<List<WorkflowGroupVO>> list(Integer status) {
        LambdaQueryWrapper<WorkflowGroup> wrapper = new LambdaQueryWrapper<WorkflowGroup>()
                .eq(status != null, WorkflowGroup::getStatus, status)
                .orderByAsc(WorkflowGroup::getSort)
                .orderByDesc(WorkflowGroup::getUpdatedTime);
        return R.ok(mapper.selectList(wrapper).stream().map(this::toVO).toList());
    }

    @Override
    public R<WorkflowGroupVO> get(Long id) {
        return R.ok(toVO(selectRequired(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> create(SaveWorkflowGroupCommand command) {
        Require.notNull(command, WorkflowCode.GROUP_INVALID);
        validate(command);
        WorkflowGroup entity = new WorkflowGroup();
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
        return R.ok(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> update(SaveWorkflowGroupCommand command) {
        Require.notNull(command, WorkflowCode.GROUP_INVALID);
        Require.notNull(command.getId(), WorkflowCode.GROUP_INVALID.getCode(), "流程分组ID不能为空");
        validate(command);
        WorkflowGroup entity = selectRequired(command.getId());
        copy(command, entity);
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedTime(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return R.ok(mapper.updateById(entity) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(Long id) {
        WorkflowGroup entity = selectRequired(id);
        Long count = definitionMapper.selectCount(new LambdaQueryWrapper<io.mango.workflow.core.entity.WorkflowDefinition>()
                .eq(io.mango.workflow.core.entity.WorkflowDefinition::getGroupId, entity.getId()));
        Require.isTrue(count == null || count == 0, WorkflowCode.GROUP_INVALID.getCode(), "流程分组下存在流程定义，不能删除");
        return R.ok(mapper.deleteById(id) > 0);
    }

    private LambdaQueryWrapper<WorkflowGroup> wrapper(WorkflowGroupPageQuery query) {
        String keyword = trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<WorkflowGroup>()
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(WorkflowGroup::getGroupName, keyword)
                        .or()
                        .like(WorkflowGroup::getGroupCode, keyword))
                .eq(query.getStatus() != null, WorkflowGroup::getStatus, query.getStatus())
                .orderByAsc(WorkflowGroup::getSort)
                .orderByDesc(WorkflowGroup::getUpdatedTime);
    }

    private WorkflowGroup selectRequired(Long id) {
        Require.notNull(id, WorkflowCode.GROUP_INVALID.getCode(), "流程分组ID不能为空");
        WorkflowGroup entity = mapper.selectById(id);
        Require.notNull(entity, WorkflowCode.GROUP_NOT_FOUND);
        return entity;
    }

    private void validate(SaveWorkflowGroupCommand command) {
        Require.notBlank(command.getGroupName(), WorkflowCode.GROUP_INVALID.getCode(), "分组名称不能为空");
        Require.notBlank(command.getGroupCode(), WorkflowCode.GROUP_INVALID.getCode(), "分组编码不能为空");
    }

    private void copy(SaveWorkflowGroupCommand command, WorkflowGroup entity) {
        entity.setGroupName(command.getGroupName().trim());
        entity.setGroupCode(command.getGroupCode().trim());
        entity.setSort(command.getSort() == null ? 0 : command.getSort());
        entity.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        entity.setRemark(trimToNull(command.getRemark()));
    }

    private WorkflowGroupVO toVO(WorkflowGroup entity) {
        WorkflowGroupVO vo = new WorkflowGroupVO();
        vo.setId(entity.getId());
        vo.setGroupName(entity.getGroupName());
        vo.setGroupCode(entity.getGroupCode());
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
