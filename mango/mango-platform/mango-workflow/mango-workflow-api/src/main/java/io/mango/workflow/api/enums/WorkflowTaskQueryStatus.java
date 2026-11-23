package io.mango.workflow.api.enums;

import lombok.Getter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 任务列表查询状态，一个枚举同时表达分组与细状态。
 *
 * <p>入参只有 {@code statuses} 一个字段，多值取并集；分组值由服务端展开成细状态，调用方不需要
 * 传第二个过滤参数。历史取值 {@code TODO}/{@code DONE}/{@code CANCELED} 语义不变，其中
 * {@code DONE} 的可见集包含「本人驳回」「本人退回」，这是排查「我驳回过什么」的默认入口。
 *
 * <p>五个细状态在 SQL 侧互斥且覆盖全量：任意一行任务恰好命中一个细状态，因此
 * {@code statuses=[DONE, CANCELED]} 的并集等于不加状态过滤时的行集。
 */
@Getter
public enum WorkflowTaskQueryStatus {

    /**
     * 待办：任务未结束且未被删除。
     */
    TODO("待办", Set.of()),

    /**
     * 通过：任务正常审批结束。
     */
    COMPLETED("通过", Set.of()),

    /**
     * 驳回：该任务上存在驳回或自动驳回处理记录。
     */
    REJECTED("驳回", Set.of()),

    /**
     * 退回：该任务上存在退回处理记录（且不是驳回）。
     */
    RETURNED("退回", Set.of()),

    /**
     * 撤回：申请被撤回导致该任务终止。
     */
    WITHDRAWN("撤回", Set.of()),

    /**
     * 流程终止：任务被删除、本人既没驳回也没退回，且申请状态不是撤回。
     *
     * <p>覆盖「申请被终止/取消」与「被他人驳回牵连作废」两种情况，也用兜底桶吸收历史数据与
     * 外部系统直接删除流程实例的行——申请状态取值再多也不会漏行。
     */
    TERMINATED("流程终止", Set.of()),

    /**
     * 已办分组：通过、驳回、退回。
     */
    DONE("已办", Set.of(COMPLETED, REJECTED, RETURNED)),

    /**
     * 已终止分组：撤回、流程终止。
     */
    CANCELED("已终止", Set.of(WITHDRAWN, TERMINATED));

    private final String label;
    private final Set<WorkflowTaskQueryStatus> groupMembers;

    WorkflowTaskQueryStatus(String label, Set<WorkflowTaskQueryStatus> groupMembers) {
        this.label = label;
        this.groupMembers = groupMembers;
    }

    /**
     * 是否为分组值。
     *
     * @return 分组返回 true，细状态返回 false
     */
    public boolean isGroup() {
        return !groupMembers.isEmpty();
    }

    /**
     * 展开成细状态集合。
     *
     * @return 分组返回其成员，细状态返回仅包含自身的集合
     */
    public Set<WorkflowTaskQueryStatus> expand() {
        return isGroup() ? groupMembers : Set.of(this);
    }

    /**
     * 把可能含分组值的状态集合展开成细状态集合。
     *
     * @param statuses 调用方传入的状态，可为 {@code null}
     * @return 去重后的细状态集合，保持首次出现顺序
     */
    public static List<WorkflowTaskQueryStatus> expand(Collection<WorkflowTaskQueryStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<WorkflowTaskQueryStatus> expanded = new LinkedHashSet<>();
        for (WorkflowTaskQueryStatus status : statuses) {
            if (status == null) {
                continue;
            }
            expanded.addAll(status.expand());
        }
        return List.copyOf(expanded);
    }
}
