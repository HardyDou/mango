package io.mango.workflow.core.service.impl;

import io.mango.infra.persistence.api.scope.DataScopeProvider;
import io.mango.infra.persistence.api.scope.DataScopeRule;
import io.mango.workflow.api.enums.WorkflowTaskQueryScope;
import io.mango.workflow.core.model.WorkflowTaskAssigneeScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 把平台数据范围规则翻译成任务列表的处理人可见集。
 *
 * <p>只读 {@link DataScopeProvider#resolve(String)}，不改 {@code DataScopeApplier} 的签名：
 * 任务是引擎历史表加自定义 SQL，不是 MyBatis-Plus wrapper，无法复用现成的 applier。
 *
 * <p>规则语义（与 {@code MybatisPlusDataScopeApplier} 对齐，但作用对象是「处理人」而不是记录创建人）：
 * <ul>
 *   <li>{@code ALL}：持 {@code workflow:task:all} 时不收窄；不持有时退化成本人，避免「只配了列表权限却看到全租户」；</li>
 *   <li>{@code SELF} 或未配置：沿用既有的本人口径（办理人 ∪ 候选），与现网默认完全一致；</li>
 *   <li>{@code ORG}：处理人所在组织 ∈ 规则值（provider 已递归展开），按 {@code selfIncluded} 决定是否并上本人；</li>
 *   <li>{@code ORG} 无值且不含本人：整体全拒，漏配等于什么都看不到；</li>
 *   <li>{@code TENANT}/{@code CUSTOM}：退化成本人并告警，列表页 500 比降级更糟。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowTaskDataScopeFilter {

    /**
     * 任务列表的数据范围资源码。
     */
    public static final String DATA_SCOPE_RESOURCE_CODE = "workflow:task:list";

    private final ObjectProvider<DataScopeProvider> dataScopeProviderProvider;

    /**
     * 解析处理人可见集。
     *
     * @param scope        调用方声明的查询范围
     * @param canQueryAll  当前主体是否持有 {@code workflow:task:all}
     * @param selfIdentifiers 当前用户标识（用户名与用户 ID）
     * @param selfCandidateGroups 当前用户所属候选组
     * @return 需要收窄时返回处理人可见集；返回 {@code null} 表示沿用调用方的既有语义
     *         （管理视图不收窄，或本人口径由调用方原本的 selfScope 分支处理）
     */
    public WorkflowTaskAssigneeScope resolve(WorkflowTaskQueryScope scope, boolean canQueryAll,
                                             List<String> selfIdentifiers, List<String> selfCandidateGroups) {
        if (scope == WorkflowTaskQueryScope.ALL && canQueryAll) {
            return null;
        }
        DataScopeProvider provider = dataScopeProviderProvider.getIfAvailable();
        if (provider == null) {
            return null;
        }
        Optional<DataScopeRule> resolved;
        try {
            resolved = provider.resolve(DATA_SCOPE_RESOURCE_CODE);
        } catch (RuntimeException ex) {
            // 缺少登录主体（内部接口、定时任务复用同一 service）时 provider 会直接抛错；
            // 这里只吞「解析不到」这一类，退化回调用方既有口径，不把列表打成 500。
            log.warn("任务列表数据范围解析失败，按调用方既有口径处理: resourceCode={}",
                    DATA_SCOPE_RESOURCE_CODE, ex);
            return null;
        }
        if (resolved == null || resolved.isEmpty()) {
            return null;
        }
        DataScopeRule rule = resolved.get();
        DataScopeRule.Mode mode = rule.mode();
        if (mode == DataScopeRule.Mode.SELF) {
            return null;
        }
        if (mode == DataScopeRule.Mode.ALL) {
            if (canQueryAll) {
                return null;
            }
            log.warn("任务列表数据范围配置为全部数据，但当前主体缺少 {} 权限，已退化成本人范围",
                    WorkflowTaskQueryAuthorizer.QUERY_ALL_PERMISSION);
            return null;
        }
        if (mode == DataScopeRule.Mode.ORG) {
            List<String> orgIds = rule.values() == null ? List.of() : List.copyOf(rule.values());
            if (orgIds.isEmpty() && !rule.selfIncluded()) {
                log.warn("任务列表数据范围配置为组织范围，但没有配置组织且不含本人，本次查询不返回任何任务");
                return WorkflowTaskAssigneeScope.DENY_ALL;
            }
            return new WorkflowTaskAssigneeScope(false, orgIds,
                    rule.selfIncluded() ? selfIdentifiers : List.of(),
                    selfIdentifiers, selfCandidateGroups);
        }
        log.warn("任务列表数据范围配置为不支持的 {} 模式，已退化成本人范围", mode);
        return null;
    }
}
