package io.mango.workflow.core.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.workflow.api.enums.WorkflowTaskAction;
import io.mango.workflow.api.enums.WorkflowTaskAssigneeSource;
import io.mango.workflow.core.entity.WorkflowTaskRecordEntity;
import io.mango.workflow.core.mapper.WorkflowTaskRecordMapper;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 任务承办人来源解析。
 *
 * <p>平台在每次写入 assignee 时把来源落到任务级变量，避免用 Flowable 的 {@code claim_time_} 反推：
 * 引擎的 {@code setAssignee} 不写领取时间，只看领取时间为空会把平台派单任务当成「非认领获得」并拒绝释放。
 * 升级前创建的任务没有该变量，按 {@code workflow_task_record} 里该任务最近一条认领或派单记录推导；
 * 推导不出时按「流程定义指定」处理，即不允许承办人释放。
 */
@Service
@RequiredArgsConstructor
public class WorkflowTaskAssigneeSourceResolver {

    /** 任务级变量名：当前 assignee 的来源。 */
    public static final String ASSIGN_SOURCE_VARIABLE = "mangoAssignSource";

    /** 任务级变量名：任务已被承办人释放回候选池，运行时不再对它自动派单。 */
    public static final String RELEASED_TO_POOL_VARIABLE = "mangoReleasedToPool";

    private static final List<String> LEGACY_ACTIONS = List.of(
            WorkflowTaskAction.CLAIM.name(), WorkflowTaskAction.AUTO_ASSIGN.name());

    private final TaskService taskService;
    private final WorkflowTaskRecordMapper taskRecordMapper;

    /** 读取任务当前承办人来源；未指派任务返回 null。 */
    public WorkflowTaskAssigneeSource resolve(Task task) {
        if (task == null || !StringUtils.hasText(task.getAssignee())) {
            return null;
        }
        WorkflowTaskAssigneeSource recorded = WorkflowTaskAssigneeSource.fromCode(
                stringVariable(task.getId(), ASSIGN_SOURCE_VARIABLE));
        return recorded == null ? resolveFromRecords(task.getId()) : recorded;
    }

    /** 记录任务当前承办人来源。 */
    public void mark(Task task, WorkflowTaskAssigneeSource source) {
        if (task == null || source == null) {
            return;
        }
        taskService.setVariableLocal(task.getId(), ASSIGN_SOURCE_VARIABLE, source.name());
    }

    /** 任务是否已被承办人释放回候选池。 */
    public boolean isReleasedToPool(Task task) {
        return task != null && Boolean.TRUE.equals(
                taskService.getVariableLocal(task.getId(), RELEASED_TO_POOL_VARIABLE));
    }

    private WorkflowTaskAssigneeSource resolveFromRecords(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            return WorkflowTaskAssigneeSource.DEFINITION;
        }
        WorkflowTaskRecordEntity latest = taskRecordMapper.selectOne(new LambdaQueryWrapper<WorkflowTaskRecordEntity>()
                .eq(WorkflowTaskRecordEntity::getTaskId, taskId)
                .eq(StringUtils.hasText(MangoContextHolder.tenantId()),
                        WorkflowTaskRecordEntity::getTenantId, MangoContextHolder.tenantId())
                .in(WorkflowTaskRecordEntity::getAction, LEGACY_ACTIONS)
                .orderByDesc(WorkflowTaskRecordEntity::getId)
                .last("limit 1"));
        if (latest == null) {
            return WorkflowTaskAssigneeSource.DEFINITION;
        }
        return WorkflowTaskAction.AUTO_ASSIGN.name().equals(latest.getAction())
                ? WorkflowTaskAssigneeSource.AUTO_ASSIGN
                : WorkflowTaskAssigneeSource.SELF_CLAIM;
    }

    private Object readVariable(String taskId, String name) {
        return taskService.getVariableLocal(taskId, name);
    }

    private String stringVariable(String taskId, String name) {
        Object value = readVariable(taskId, name);
        return value == null ? null : String.valueOf(value);
    }
}
