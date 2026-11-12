package io.mango.workflow.core.service.impl;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.vo.AuthorizationSnapshotVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 任务查询权限判定。
 *
 * <p>全量任务查询属于管理视图，不识别角色名称，只认 {@code workflow:task:all} 权限码；
 * 未安装授权中心时该范围始终拒绝，避免默认放开全量查询。
 */
@Component
@RequiredArgsConstructor
public class WorkflowTaskQueryAuthorizer {

    /**
     * 全量任务查询权限码。
     */
    public static final String QUERY_ALL_PERMISSION = "workflow:task:all";

    private final ObjectProvider<IAuthorizationProvider> authorizationProvider;

    /**
     * 判断当前主体是否具备全量任务查询权限。
     *
     * @return 具备权限返回 true；缺少授权能力、缺少主体上下文或权限码不匹配时返回 false
     */
    public boolean canQueryAll() {
        IAuthorizationProvider provider = authorizationProvider.getIfAvailable();
        if (provider == null) {
            return false;
        }
        MangoContextSnapshot context = MangoContextHolder.get();
        if (context == null || context.memberId() == null) {
            return false;
        }
        AuthorizationSnapshotVO snapshot = provider.load(AuthorizationQuery.member(context.memberId())
                .withTenantId(context.tenantId())
                .withSystemCode(context.appCode())
                .withRealm(context.realm())
                .withActorType(context.actorType())
                .withParty(context.partyType(), context.partyId()));
        return snapshot != null && snapshot.getPermissionCodes().contains(QUERY_ALL_PERMISSION);
    }
}
