package io.mango.authorization.starter;

import io.mango.authorization.api.enums.DataScopeMode;
import io.mango.authorization.api.vo.EffectiveDataScopeVO;
import io.mango.authorization.core.service.IRoleDataScopeService;
import io.mango.common.result.R;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.vo.TenantMemberInfo;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.api.scope.DataScopeRule;
import io.mango.org.api.SysOrgApi;
import io.mango.org.api.entity.SysOrg;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AuthorizationDataScopeProvider 测试")
class AuthorizationDataScopeProviderTest {

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("resolve should convert SELF_ORG to current member primary org")
    void resolve_selfOrg_usesPrimaryOrg() {
        IRoleDataScopeService roleDataScopeService = mock(IRoleDataScopeService.class);
        TenantMemberProvider tenantMemberProvider = mock(TenantMemberProvider.class);
        ObjectProvider<SysOrgApi> sysOrgApiProvider = mock(ObjectProvider.class);
        when(roleDataScopeService.resolve(any(), eq("workflow:definition:list"))).thenReturn(scope(DataScopeMode.SELF_ORG));
        when(tenantMemberProvider.getEnabledMember(1L, 2L)).thenReturn(member(100L));
        setContext();

        AuthorizationDataScopeProvider provider = new AuthorizationDataScopeProvider(
                roleDataScopeService,
                tenantMemberProvider,
                sysOrgApiProvider);

        Optional<DataScopeRule> result = provider.resolve("workflow:definition:list");

        assertThat(result).isPresent();
        assertThat(result.get().mode()).isEqualTo(DataScopeRule.Mode.ORG);
        assertThat(result.get().values()).containsExactly("100");
    }

    @Test
    @DisplayName("resolve should expand SELF_ORG_AND_CHILDREN recursively")
    void resolve_selfOrgAndChildren_expandsChildren() {
        IRoleDataScopeService roleDataScopeService = mock(IRoleDataScopeService.class);
        TenantMemberProvider tenantMemberProvider = mock(TenantMemberProvider.class);
        ObjectProvider<SysOrgApi> sysOrgApiProvider = mock(ObjectProvider.class);
        SysOrgApi sysOrgApi = mock(SysOrgApi.class);
        when(roleDataScopeService.resolve(any(), eq("workflow:definition:list")))
                .thenReturn(scope(DataScopeMode.SELF_ORG_AND_CHILDREN));
        when(tenantMemberProvider.getEnabledMember(1L, 2L)).thenReturn(member(100L));
        when(sysOrgApiProvider.getIfAvailable()).thenReturn(sysOrgApi);
        when(sysOrgApi.children(100L)).thenReturn(R.ok(List.of(org(101L), org(102L))));
        when(sysOrgApi.children(101L)).thenReturn(R.ok(List.of(org(103L))));
        when(sysOrgApi.children(102L)).thenReturn(R.ok(List.of()));
        when(sysOrgApi.children(103L)).thenReturn(R.ok(List.of()));
        setContext();

        AuthorizationDataScopeProvider provider = new AuthorizationDataScopeProvider(
                roleDataScopeService,
                tenantMemberProvider,
                sysOrgApiProvider);

        Optional<DataScopeRule> result = provider.resolve("workflow:definition:list");

        assertThat(result).isPresent();
        assertThat(result.get().mode()).isEqualTo(DataScopeRule.Mode.ORG);
        assertThat(result.get().values()).containsExactlyInAnyOrder("100", "101", "102", "103");
    }

    private EffectiveDataScopeVO scope(DataScopeMode mode) {
        EffectiveDataScopeVO scope = new EffectiveDataScopeVO();
        scope.setScopeMode(mode);
        scope.setSelfIncluded(false);
        return scope;
    }

    private TenantMemberInfo member(Long primaryOrgId) {
        TenantMemberInfo member = new TenantMemberInfo();
        member.setUserId(1L);
        member.setTenantId(2L);
        member.setMemberId(1001L);
        member.setPrimaryOrgId(primaryOrgId);
        return member;
    }

    private SysOrg org(Long id) {
        SysOrg org = new SysOrg();
        org.setId(id);
        return org;
    }

    private void setContext() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1L,
                1001L,
                "2",
                "admin",
                "INTERNAL",
                "INTERNAL_USER",
                "INTERNAL_ORG",
                2L,
                "internal-admin"));
    }
}
