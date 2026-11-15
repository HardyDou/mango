package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.mango.workflow.api.enums.WorkflowTaskAction;
import io.mango.workflow.api.enums.WorkflowTaskClaimStatus;
import io.mango.workflow.api.query.WorkflowTaskPageQuery;
import io.mango.workflow.api.vo.WorkflowBusinessApplyCurrentTaskVO;
import io.mango.workflow.api.vo.WorkflowBusinessApplyVO;
import io.mango.workflow.api.vo.WorkflowJsonVO;
import io.mango.workflow.api.vo.WorkflowTaskActionResultVO;
import io.mango.workflow.api.vo.WorkflowTaskCompleteResultVO;
import io.mango.workflow.api.vo.WorkflowTaskDetailVO;
import io.mango.workflow.core.entity.WorkflowBusinessApplyEntity;
import io.mango.workflow.core.entity.WorkflowDefinitionEntity;
import io.mango.workflow.core.entity.WorkflowDefinitionVersionEntity;
import io.mango.workflow.core.entity.WorkflowFormInstanceEntity;
import io.mango.workflow.core.mapper.WorkflowBusinessApplyMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionVersionMapper;
import io.mango.workflow.core.service.WorkflowTaskAdvanceResult;
import lombok.RequiredArgsConstructor;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务动作结果与详情视图装配。
 *
 * <p>把「已处理任务 + 业务申请快照」映射成动作结果视图、把表单与流程定义快照映射成任务详情视图，
 * 与任务运行时状态机解耦，避免运行时服务同时承担编排和视图装配。
 */
@Service
@RequiredArgsConstructor
public class WorkflowTaskViewAssembler {

    private static final String LIMIT_ONE = "limit 1";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final WorkflowBusinessApplyMapper businessApplyMapper;
    private final WorkflowDefinitionMapper definitionMapper;
    private final WorkflowDefinitionVersionMapper definitionVersionMapper;

    /** 完成类动作结果：已完成任务 + 刷新后的业务申请与当前任务。 */
    public WorkflowTaskCompleteResultVO toCompleteResult(WorkflowTaskAction action, Task previousTask,
                                                         WorkflowTaskAdvanceResult advanceResult) {
        WorkflowTaskCompleteResultVO vo = new WorkflowTaskCompleteResultVO();
        vo.setActionResult(action);
        vo.setCompletedTaskId(previousTask.getId());
        vo.setCompletedTaskName(previousTask.getName());
        vo.setCompletedTaskDefinitionKey(previousTask.getTaskDefinitionKey());
        vo.setProcessInstanceId(advanceResult.processInstanceId());
        vo.setEnded(advanceResult.ended());
        WorkflowBusinessApplyVO apply = advanceResult.businessApply();
        if (apply == null) {
            vo.setCurrentTasks(List.of());
            return vo;
        }
        vo.setApplyId(apply.getId());
        vo.setBusinessType(apply.getBusinessType());
        vo.setBusinessKey(apply.getBusinessKey());
        vo.setApplyStatus(apply.getApplyStatus());
        vo.setApplyStatusName(apply.getApplyStatusName());
        vo.setCurrentTaskNames(apply.getCurrentTaskNames());
        vo.setCurrentTaskDefinitionKeys(apply.getCurrentTaskDefinitionKeys());
        vo.setCurrentAssigneeNames(apply.getCurrentAssigneeNames());
        List<WorkflowBusinessApplyCurrentTaskVO> currentTasks = apply.getCurrentTasks() == null
                ? List.<WorkflowBusinessApplyCurrentTaskVO>of()
                : apply.getCurrentTasks();
        vo.setCurrentTasks(currentTasks);
        vo.setCancelled(WorkflowApplyStatus.CANCELED == apply.getApplyStatus());
        vo.setRejected(WorkflowApplyStatus.REJECTED == apply.getApplyStatus());
        fillCurrentTask(vo, currentTasks);
        return vo;
    }

    /** 非完成类动作结果：认领、释放、暂存、驳回等动作的前一任务与当前任务快照。 */
    public WorkflowTaskActionResultVO toActionResult(WorkflowTaskAction action, Task previousTask,
                                                     boolean ended, WorkflowBusinessApplyVO apply) {
        WorkflowTaskActionResultVO vo = new WorkflowTaskActionResultVO();
        vo.setActionResult(action);
        vo.setPreviousTaskId(previousTask.getId());
        vo.setPreviousTaskDefinitionKey(previousTask.getTaskDefinitionKey());
        vo.setPreviousTaskName(previousTask.getName());
        vo.setProcessInstanceId(previousTask.getProcessInstanceId());
        vo.setEnded(ended);
        if (apply == null) {
            vo.setNextTasks(List.of());
            vo.setCandidateUsers(List.of());
            vo.setCandidateGroups(List.of());
            vo.setClaimStatus(WorkflowTaskClaimStatus.NONE);
            return vo;
        }
        vo.setApplyId(apply.getId());
        vo.setBusinessType(apply.getBusinessType());
        vo.setBusinessKey(apply.getBusinessKey());
        vo.setProcessStatus(apply.getApplyStatus());
        vo.setProcessStatusName(apply.getApplyStatusName());
        vo.setCancelled(WorkflowApplyStatus.CANCELED == apply.getApplyStatus());
        vo.setRejected(WorkflowApplyStatus.REJECTED == apply.getApplyStatus());
        List<WorkflowBusinessApplyCurrentTaskVO> nextTasks = apply.getCurrentTasks() == null
                ? List.<WorkflowBusinessApplyCurrentTaskVO>of()
                : apply.getCurrentTasks();
        vo.setNextTasks(nextTasks);
        fillCurrentTask(vo, nextTasks);
        return vo;
    }

    /** 业务申请表单快照：表单实例优先，缺失时回落流程定义表单。 */
    public void fillForm(WorkflowTaskDetailVO vo, WorkflowFormInstanceEntity formInstance,
                         WorkflowDefinitionEntity definition, Map<String, Object> runtimeVariables) {
        vo.setFormCode(formInstance == null
                ? (definition == null ? null : definition.getFormCode()) : formInstance.getFormCode());
        vo.setFormJson(formInstance == null
                ? (definition == null ? null : definition.getFormJson()) : formInstance.getFormJson());
        vo.setVariables(WorkflowJsonVO.of(formInstance == null
                ? runtimeVariables : parseMap(formInstance.getVariablesJson())));
    }

    /** 按流程定义 ID 或变量里的定义主键解析当前定义。 */
    public WorkflowDefinitionEntity findDefinition(String processDefinitionId, Map<String, Object> variables) {
        if (StringUtils.hasText(processDefinitionId)) {
            WorkflowDefinitionEntity definition = definitionMapper.selectOne(new LambdaQueryWrapper<WorkflowDefinitionEntity>()
                    .eq(WorkflowDefinitionEntity::getProcessDefinitionId, processDefinitionId)
                    .last(LIMIT_ONE));
            if (definition != null) {
                return definition;
            }
        }
        Long definitionId = variableLong(variables, "mangoDefinitionId");
        return definitionId == null ? null : definitionMapper.selectById(definitionId);
    }

    /** 读取实例运行版本的设计器快照：发布版本优先，缺失时回落定义快照。 */
    public String findDesignerJson(String processDefinitionId, WorkflowDefinitionEntity resolvedDefinition) {
        if (!StringUtils.hasText(processDefinitionId)) {
            return null;
        }
        WorkflowDefinitionVersionEntity version = definitionVersionMapper.selectOne(
                new LambdaQueryWrapper<WorkflowDefinitionVersionEntity>()
                        .eq(WorkflowDefinitionVersionEntity::getProcessDefinitionId, processDefinitionId)
                        .orderByDesc(WorkflowDefinitionVersionEntity::getId)
                        .last(LIMIT_ONE));
        if (version != null && StringUtils.hasText(version.getDesignerJson())) {
            return version.getDesignerJson();
        }
        WorkflowDefinitionEntity definition = resolvedDefinition;
        if (definition == null || !processDefinitionId.equals(definition.getProcessDefinitionId())) {
            definition = definitionMapper.selectOne(new LambdaQueryWrapper<WorkflowDefinitionEntity>()
                    .eq(WorkflowDefinitionEntity::getProcessDefinitionId, processDefinitionId)
                    .last(LIMIT_ONE));
        }
        return definition == null || !processDefinitionId.equals(definition.getProcessDefinitionId())
                ? null
                : definition.getDesignerJson();
    }

    /** 关键字命中的业务申请流程实例；用于待办搜索按业务主键、标题或当前节点匹配。 */
    public List<String> businessProcessInstanceIds(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return businessApplyMapper.selectList(new LambdaQueryWrapper<WorkflowBusinessApplyEntity>()
                        .select(WorkflowBusinessApplyEntity::getProcessInstanceId)
                        .isNotNull(WorkflowBusinessApplyEntity::getProcessInstanceId)
                        .and(wrapper -> wrapper
                                .like(WorkflowBusinessApplyEntity::getBusinessKey, keyword)
                                .or()
                                .like(WorkflowBusinessApplyEntity::getApplyTitle, keyword)
                                .or()
                                .like(WorkflowBusinessApplyEntity::getProcessName, keyword)
                                .or()
                                .like(WorkflowBusinessApplyEntity::getCurrentTaskNames, keyword)))
                .stream()
                .map(WorkflowBusinessApplyEntity::getProcessInstanceId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    /** 分页查询对象兜底，缺省返回首页默认查询。 */
    public WorkflowTaskPageQuery resolve(WorkflowTaskPageQuery query) {
        return query == null ? new WorkflowTaskPageQuery() : query;
    }

    private void fillCurrentTask(WorkflowTaskCompleteResultVO vo, List<WorkflowBusinessApplyCurrentTaskVO> currentTasks) {
        if (currentTasks.isEmpty()) {
            vo.setClaimStatus(WorkflowTaskClaimStatus.NONE);
            vo.setCandidateUsers(List.of());
            vo.setCandidateGroups(List.of());
            return;
        }
        WorkflowBusinessApplyCurrentTaskVO first = currentTasks.getFirst();
        vo.setCurrentTaskId(first.getTaskId());
        vo.setCurrentTaskName(first.getTaskName());
        vo.setTaskDefinitionKey(first.getTaskDefinitionKey());
        vo.setAssigneeId(first.getAssigneeId());
        vo.setAssigneeName(first.getAssigneeName());
        vo.setAssigneeDisplayName(first.getAssigneeDisplayName());
        vo.setClaimStatus(first.getClaimStatus());
        vo.setCandidateUsers(first.getCandidateUsers() == null ? List.of() : first.getCandidateUsers());
        vo.setCandidateGroups(first.getCandidateGroups() == null ? List.of() : first.getCandidateGroups());
        vo.setCurrentTask(first);
    }

    private void fillCurrentTask(WorkflowTaskActionResultVO vo, List<WorkflowBusinessApplyCurrentTaskVO> currentTasks) {
        if (currentTasks.isEmpty()) {
            vo.setClaimStatus(WorkflowTaskClaimStatus.NONE);
            vo.setCandidateUsers(List.of());
            vo.setCandidateGroups(List.of());
            return;
        }
        WorkflowBusinessApplyCurrentTaskVO first = currentTasks.getFirst();
        vo.setCurrentTaskId(first.getTaskId());
        vo.setCurrentTaskName(first.getTaskName());
        vo.setTaskDefinitionKey(first.getTaskDefinitionKey());
        vo.setAssigneeId(first.getAssigneeId());
        vo.setAssigneeName(first.getAssigneeName());
        vo.setAssigneeDisplayName(first.getAssigneeDisplayName());
        vo.setClaimStatus(first.getClaimStatus());
        vo.setCandidateUsers(first.getCandidateUsers() == null ? List.of() : first.getCandidateUsers());
        vo.setCandidateGroups(first.getCandidateGroups() == null ? List.of() : first.getCandidateGroups());
        vo.setCurrentTask(first);
    }

    private Long variableLong(Map<String, Object> variables, String key) {
        Object value = variables == null ? null : variables.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (!StringUtils.hasText(String.valueOf(value))) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Map<String, Object> parseMap(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, MAP_TYPE);
            return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
        } catch (JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }
}
