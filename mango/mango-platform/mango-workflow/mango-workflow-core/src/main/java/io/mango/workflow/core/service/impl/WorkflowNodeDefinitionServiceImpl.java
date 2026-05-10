package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.workflow.api.WorkflowCode;
import io.mango.workflow.api.command.SaveWorkflowNodeDefinitionCommand;
import io.mango.workflow.api.command.UpdateWorkflowNodeDefinitionStatusCommand;
import io.mango.workflow.api.query.WorkflowNodeDefinitionPageQuery;
import io.mango.workflow.api.vo.WorkflowNodeDefinitionVO;
import io.mango.workflow.core.entity.WorkflowNodeDefinition;
import io.mango.workflow.core.mapper.WorkflowNodeDefinitionMapper;
import io.mango.workflow.core.service.IWorkflowNodeDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 流程节点定义服务实现。
 */
@Service
@RequiredArgsConstructor
public class WorkflowNodeDefinitionServiceImpl implements IWorkflowNodeDefinitionService {

    private static final Set<String> BPMN_TYPES = Set.of(
            "startEvent", "userTask", "serviceTask", "exclusiveGateway", "parallelGateway");
    private static final Set<String> EXECUTION_TYPES = Set.of(
            "NONE", "USER_TASK", "SPRING_BEAN", "HTTP_URL", "REMOTE_SERVICE", "EVENT_PUBLISH");

    private final WorkflowNodeDefinitionMapper mapper;

    @Override
    public R<PageResult<WorkflowNodeDefinitionVO>> page(WorkflowNodeDefinitionPageQuery query) {
        WorkflowNodeDefinitionPageQuery resolved = query == null ? new WorkflowNodeDefinitionPageQuery() : query;
        IPage<WorkflowNodeDefinition> page = mapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                wrapper(resolved));
        List<WorkflowNodeDefinitionVO> records = page.getRecords().stream().map(this::toVO).toList();
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<List<WorkflowNodeDefinitionVO>> list(Integer status) {
        List<WorkflowNodeDefinitionVO> records = mapper.selectList(new LambdaQueryWrapper<WorkflowNodeDefinition>()
                        .eq(status != null, WorkflowNodeDefinition::getStatus, status)
                        .orderByAsc(WorkflowNodeDefinition::getCategoryCode)
                        .orderByAsc(WorkflowNodeDefinition::getSort)
                        .orderByAsc(WorkflowNodeDefinition::getId))
                .stream()
                .map(this::toVO)
                .toList();
        return R.ok(records);
    }

    @Override
    public R<WorkflowNodeDefinitionVO> get(Long id) {
        return R.ok(toVO(selectRequired(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> create(SaveWorkflowNodeDefinitionCommand command) {
        Require.notNull(command, WorkflowCode.NODE_DEFINITION_INVALID);
        validate(command, false);
        WorkflowNodeDefinition entity = new WorkflowNodeDefinition();
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
    public R<Boolean> update(SaveWorkflowNodeDefinitionCommand command) {
        Require.notNull(command, WorkflowCode.NODE_DEFINITION_INVALID);
        Require.notNull(command.getId(), WorkflowCode.NODE_DEFINITION_INVALID.getCode(), "节点定义ID不能为空");
        validate(command, true);
        WorkflowNodeDefinition entity = selectRequired(command.getId());
        copy(command, entity);
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedTime(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return R.ok(mapper.updateById(entity) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateStatus(UpdateWorkflowNodeDefinitionStatusCommand command) {
        Require.notNull(command, WorkflowCode.NODE_DEFINITION_INVALID);
        Require.notNull(command.getId(), WorkflowCode.NODE_DEFINITION_INVALID.getCode(), "节点定义ID不能为空");
        Require.notNull(command.getStatus(), WorkflowCode.NODE_DEFINITION_INVALID.getCode(), "节点定义状态不能为空");
        Require.isTrue(command.getStatus() == 0 || command.getStatus() == 1,
                WorkflowCode.NODE_DEFINITION_INVALID.getCode(), "节点定义状态只能是0或1");
        WorkflowNodeDefinition entity = selectRequired(command.getId());
        entity.setStatus(command.getStatus());
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedTime(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return R.ok(mapper.updateById(entity) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(Long id) {
        WorkflowNodeDefinition entity = selectRequired(id);
        return R.ok(mapper.deleteById(entity.getId()) > 0);
    }

    private LambdaQueryWrapper<WorkflowNodeDefinition> wrapper(WorkflowNodeDefinitionPageQuery query) {
        String keyword = trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<WorkflowNodeDefinition>()
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(WorkflowNodeDefinition::getNodeName, keyword)
                        .or()
                        .like(WorkflowNodeDefinition::getNodeDefinitionCode, keyword)
                        .or()
                        .like(WorkflowNodeDefinition::getNodeType, keyword))
                .eq(StringUtils.hasText(query.getCategoryCode()), WorkflowNodeDefinition::getCategoryCode, trimToNull(query.getCategoryCode()))
                .eq(StringUtils.hasText(query.getBpmnType()), WorkflowNodeDefinition::getBpmnType, trimToNull(query.getBpmnType()))
                .eq(StringUtils.hasText(query.getExecutionType()), WorkflowNodeDefinition::getExecutionType, upper(query.getExecutionType()))
                .eq(query.getStatus() != null, WorkflowNodeDefinition::getStatus, query.getStatus())
                .orderByAsc(WorkflowNodeDefinition::getCategoryCode)
                .orderByAsc(WorkflowNodeDefinition::getSort)
                .orderByDesc(WorkflowNodeDefinition::getUpdatedTime);
    }

    private WorkflowNodeDefinition selectRequired(Long id) {
        Require.notNull(id, WorkflowCode.NODE_DEFINITION_INVALID.getCode(), "节点定义ID不能为空");
        WorkflowNodeDefinition entity = mapper.selectById(id);
        Require.notNull(entity, WorkflowCode.NODE_DEFINITION_NOT_FOUND);
        return entity;
    }

    private void validate(SaveWorkflowNodeDefinitionCommand command, boolean update) {
        Require.notBlank(command.getNodeDefinitionCode(), WorkflowCode.NODE_DEFINITION_INVALID.getCode(), "节点定义编码不能为空");
        Require.notBlank(command.getNodeType(), WorkflowCode.NODE_DEFINITION_INVALID.getCode(), "节点类型不能为空");
        Require.notBlank(command.getNodeName(), WorkflowCode.NODE_DEFINITION_INVALID.getCode(), "节点名称不能为空");
        Require.notBlank(command.getCategoryCode(), WorkflowCode.NODE_DEFINITION_INVALID.getCode(), "节点分类编码不能为空");
        Require.notBlank(command.getCategoryName(), WorkflowCode.NODE_DEFINITION_INVALID.getCode(), "节点分类名称不能为空");
        Require.notBlank(command.getBpmnType(), WorkflowCode.NODE_DEFINITION_INVALID.getCode(), "底层BPMN类型不能为空");
        Require.notBlank(command.getExecutionType(), WorkflowCode.NODE_DEFINITION_INVALID.getCode(), "执行类型不能为空");
        Require.isTrue(BPMN_TYPES.contains(command.getBpmnType().trim()),
                WorkflowCode.NODE_DEFINITION_INVALID.getCode(), "底层BPMN类型不支持");
        Require.isTrue(EXECUTION_TYPES.contains(upper(command.getExecutionType())),
                WorkflowCode.NODE_DEFINITION_INVALID.getCode(), "执行类型不支持");
        Require.notNull(command.getStatus(), WorkflowCode.NODE_DEFINITION_INVALID.getCode(), "节点定义状态不能为空");
        Require.isTrue(command.getStatus() == 0 || command.getStatus() == 1,
                WorkflowCode.NODE_DEFINITION_INVALID.getCode(), "节点定义状态只能是0或1");
        Long count = mapper.selectCount(new LambdaQueryWrapper<WorkflowNodeDefinition>()
                .eq(WorkflowNodeDefinition::getNodeDefinitionCode, upper(command.getNodeDefinitionCode()))
                .ne(update && command.getId() != null, WorkflowNodeDefinition::getId, command.getId()));
        Require.isTrue(count == null || count == 0, WorkflowCode.NODE_DEFINITION_CODE_DUPLICATED);
    }

    private void copy(SaveWorkflowNodeDefinitionCommand command, WorkflowNodeDefinition entity) {
        entity.setNodeDefinitionCode(upper(command.getNodeDefinitionCode()));
        entity.setNodeType(upper(command.getNodeType()));
        entity.setNodeName(command.getNodeName().trim());
        entity.setCategoryCode(upper(command.getCategoryCode()));
        entity.setCategoryName(command.getCategoryName().trim());
        entity.setDescription(trimToNull(command.getDescription()));
        entity.setBpmnType(command.getBpmnType().trim());
        entity.setExecutionType(upper(command.getExecutionType()));
        entity.setColor(trimToNull(command.getColor()));
        entity.setIcon(trimToNull(command.getIcon()));
        entity.setPropertySchema(trimToNull(command.getPropertySchema()));
        entity.setDefaultProperties(trimToNull(command.getDefaultProperties()));
        entity.setSort(command.getSort() == null ? 0 : command.getSort());
        entity.setStatus(command.getStatus());
    }

    private WorkflowNodeDefinitionVO toVO(WorkflowNodeDefinition entity) {
        WorkflowNodeDefinitionVO vo = new WorkflowNodeDefinitionVO();
        vo.setId(entity.getId());
        vo.setNodeDefinitionCode(entity.getNodeDefinitionCode());
        vo.setNodeType(entity.getNodeType());
        vo.setNodeName(entity.getNodeName());
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setCategoryName(entity.getCategoryName());
        vo.setDescription(entity.getDescription());
        vo.setBpmnType(entity.getBpmnType());
        vo.setExecutionType(entity.getExecutionType());
        vo.setColor(entity.getColor());
        vo.setIcon(entity.getIcon());
        vo.setPropertySchema(entity.getPropertySchema());
        vo.setDefaultProperties(entity.getDefaultProperties());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
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

    private String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
