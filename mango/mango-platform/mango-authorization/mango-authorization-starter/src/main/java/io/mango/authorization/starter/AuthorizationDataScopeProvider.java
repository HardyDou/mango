package io.mango.authorization.starter;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.enums.DataScopeMode;
import io.mango.authorization.api.vo.EffectiveDataScopeVO;
import io.mango.authorization.core.service.IRoleDataScopeService;
import io.mango.common.result.R;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.vo.TenantMemberInfo;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.infra.persistence.api.scope.DataScopeProvider;
import io.mango.infra.persistence.api.scope.DataScopeRule;
import io.mango.org.api.SysOrgApi;
import io.mango.org.api.entity.SysOrg;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * 基于授权中心角色配置的数据权限提供者。
 */
@RequiredArgsConstructor
public class AuthorizationDataScopeProvider implements DataScopeProvider {

    private final IRoleDataScopeService roleDataScopeService;
    private final TenantMemberProvider tenantMemberProvider;
    private final ObjectProvider<SysOrgApi> sysOrgApiProvider;

    @Override
    public Optional<DataScopeRule> resolve(String resourceCode) {
        MangoContextSnapshot context = MangoContextHolder.get();
        if (context.memberId() == null) {
            throw new IllegalStateException("Missing member context for data scope resolution.");
        }
        AuthorizationQuery query = AuthorizationQuery.member(context.memberId())
                .withTenantId(context.tenantId())
                .withSystemCode(context.appCode())
                .withRealm(context.realm())
                .withActorType(context.actorType())
                .withParty(context.partyType(), context.partyId());
        EffectiveDataScopeVO scope = roleDataScopeService.resolve(query, resourceCode);
        if (scope == null || scope.getScopeMode() == null) {
            return Optional.of(new DataScopeRule(DataScopeRule.Mode.SELF, java.util.Set.of(), true));
        }
        return Optional.of(toRule(scope, context));
    }

    private DataScopeRule toRule(EffectiveDataScopeVO scope, MangoContextSnapshot context) {
        DataScopeRule.Mode mode = switch (scope.getScopeMode()) {
            case ALL -> DataScopeRule.Mode.ALL;
            case ORG -> DataScopeRule.Mode.ORG;
            case SELF_ORG, SELF_ORG_AND_CHILDREN -> DataScopeRule.Mode.ORG;
            case SELF -> DataScopeRule.Mode.SELF;
        };
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (scope.getScopeValues() != null) {
            scope.getScopeValues().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(values::add);
        }
        if (scope.getScopeMode() == DataScopeMode.SELF_ORG
                || scope.getScopeMode() == DataScopeMode.SELF_ORG_AND_CHILDREN) {
            Long primaryOrgId = resolvePrimaryOrgId(context);
            if (primaryOrgId != null) {
                values.add(String.valueOf(primaryOrgId));
                if (scope.getScopeMode() == DataScopeMode.SELF_ORG_AND_CHILDREN) {
                    collectChildren(primaryOrgId, values);
                }
            }
        }
        boolean selfIncluded = scope.getScopeMode() == DataScopeMode.SELF || Boolean.TRUE.equals(scope.getSelfIncluded());
        return new DataScopeRule(mode, values, selfIncluded);
    }

    private Long resolvePrimaryOrgId(MangoContextSnapshot context) {
        Long tenantId = parseTenantId(context.tenantId());
        TenantMemberInfo member = tenantMemberProvider.getEnabledMember(context.userId(), tenantId);
        if (member == null || member.getPrimaryOrgId() == null) {
            return null;
        }
        return member.getPrimaryOrgId();
    }

    private void collectChildren(Long parentId, LinkedHashSet<String> values) {
        SysOrgApi sysOrgApi = sysOrgApiProvider.getIfAvailable();
        if (sysOrgApi == null) {
            throw new IllegalStateException("SysOrgApi is required for SELF_ORG_AND_CHILDREN data scope.");
        }
        R<List<SysOrg>> response = sysOrgApi.children(parentId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return;
        }
        for (SysOrg child : response.getData()) {
            if (child == null || child.getId() == null) {
                continue;
            }
            if (values.add(String.valueOf(child.getId()))) {
                collectChildren(child.getId(), values);
            }
        }
    }

    private Long parseTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return null;
        }
        try {
            return Long.parseLong(tenantId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
