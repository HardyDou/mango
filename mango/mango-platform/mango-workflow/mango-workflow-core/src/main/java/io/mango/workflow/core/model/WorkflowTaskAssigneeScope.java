package io.mango.workflow.core.model;

import java.util.List;

/**
 * 任务列表的处理人可见集，由平台数据范围规则渲染而成。
 *
 * <p>只描述「能看见谁的任务」，不描述任务状态：四个维度之间取并集，
 * 为空表示该维度不参与匹配，{@code denyAll} 表示整体全拒（漏配组织范围时「什么都看不到」）。
 *
 * <p>值为字符串而不是 ID：组织范围来自 {@code DataScopeRule#values()}，办理人标识可能是
 * 用户名也可能是字符串化的用户 ID，两者最终都在 SQL 里按字符串比较。
 *
 * @param denyAll 是否整体拒绝
 * @param orgIds 处理人所在组织 ID 集合
 * @param selfIdentifiers 当前用户自身的标识（用户名与用户 ID）
 * @param candidateIdentifiers 当前用户作为候选人时的标识
 * @param candidateGroups 当前用户所属候选组
 */
public record WorkflowTaskAssigneeScope(boolean denyAll,
                                        List<String> orgIds,
                                        List<String> selfIdentifiers,
                                        List<String> candidateIdentifiers,
                                        List<String> candidateGroups) {

    /**
     * 全拒范围：组织档被配成「无值且不含本人」时使用。
     */
    public static final WorkflowTaskAssigneeScope DENY_ALL =
            new WorkflowTaskAssigneeScope(true, List.of(), List.of(), List.of(), List.of());

    public WorkflowTaskAssigneeScope {
        orgIds = orgIds == null ? List.of() : List.copyOf(orgIds);
        selfIdentifiers = selfIdentifiers == null ? List.of() : List.copyOf(selfIdentifiers);
        candidateIdentifiers = candidateIdentifiers == null ? List.of() : List.copyOf(candidateIdentifiers);
        candidateGroups = candidateGroups == null ? List.of() : List.copyOf(candidateGroups);
    }
}
