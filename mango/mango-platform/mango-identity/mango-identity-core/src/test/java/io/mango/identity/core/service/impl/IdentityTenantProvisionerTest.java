package io.mango.identity.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.mango.authorization.api.command.SubjectRoleBindingCommand;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.command.AddTenantMemberOrgCommand;
import io.mango.identity.api.command.UpdateTenantMemberOrgCommand;
import io.mango.identity.api.vo.TenantMemberOrgRelationVO;
import io.mango.identity.core.adapter.AuthorizationRoleBindingAdapter;
import io.mango.identity.core.entity.TenantMemberEntity;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberLifecycleLogMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.org.api.OrgReferenceProvider;
import io.mango.system.api.tenant.TenantProvisionCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityTenantProvisionerTest {

    private IdentityTenantProvisioner provisioner;

    @Mock
    private IdentityUserMapper identityUserMapper;

    @Mock
    private TenantMemberMapper tenantMemberMapper;

    @Mock
    private TenantMemberLifecycleLogMapper lifecycleLogMapper;

    @Mock
    private AuthorizationRoleBindingAdapter roleBindingAdapter;

    @Mock
    private TenantMemberProvider tenantMemberProvider;

    @Mock
    private OrgReferenceProvider orgReferenceProvider;

    @Mock
    private ObjectProvider<TenantMemberProvider> tenantMemberProviderObjectProvider;

    @Mock
    private ObjectProvider<OrgReferenceProvider> orgReferenceProviderObjectProvider;

    @BeforeEach
    void setUp() {
        provisioner = new IdentityTenantProvisioner(identityUserMapper, tenantMemberMapper,
                lifecycleLogMapper, roleBindingAdapter, tenantMemberProviderObjectProvider,
                orgReferenceProviderObjectProvider);
        when(tenantMemberProviderObjectProvider.getIfAvailable()).thenReturn(tenantMemberProvider);
        when(orgReferenceProviderObjectProvider.getIfAvailable()).thenReturn(orgReferenceProvider);
        when(orgReferenceProvider.resolveRootOrgId(2L)).thenReturn(200L);
        when(roleBindingAdapter.findRoleId(any())).thenReturn(88L);
        MangoContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void addsRootRelationAsPrimaryWhenAdminHasNoPrimaryOrg() {
        TenantMemberEntity admin = adminMember(1001L, null);
        when(tenantMemberMapper.selectList(any(Wrapper.class))).thenReturn(List.of(admin));
        when(tenantMemberProvider.listOrgRelations(2L, 200L)).thenReturn(List.of());

        provisioner.provision(new TenantProvisionCommand(2L, "company_a", "A公司"));

        ArgumentCaptor<AddTenantMemberOrgCommand> captor = ArgumentCaptor.forClass(AddTenantMemberOrgCommand.class);
        verify(tenantMemberProvider).addOrgRelation(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(2L);
        assertThat(captor.getValue().getMemberId()).isEqualTo(1001L);
        assertThat(captor.getValue().getOrgId()).isEqualTo(200L);
        assertThat(captor.getValue().getPrimaryFlag()).isTrue();
        verify(roleBindingAdapter).ensureSubjectRoleBinding(any(SubjectRoleBindingCommand.class));
    }

    @Test
    void promotesExistingRootRelationWhenPrimaryOrgIsMissing() {
        TenantMemberEntity admin = adminMember(1001L, null);
        TenantMemberOrgRelationVO relation = new TenantMemberOrgRelationVO();
        relation.setRelationId(5001L);
        relation.setMemberId(1001L);
        relation.setPostId(3001L);
        relation.setLeaderFlag(true);
        when(tenantMemberMapper.selectList(any(Wrapper.class))).thenReturn(List.of(admin));
        when(tenantMemberProvider.listOrgRelations(2L, 200L)).thenReturn(List.of(relation));

        provisioner.provision(new TenantProvisionCommand(2L, "company_a", "A公司"));

        ArgumentCaptor<UpdateTenantMemberOrgCommand> captor =
                ArgumentCaptor.forClass(UpdateTenantMemberOrgCommand.class);
        verify(tenantMemberProvider).updateOrgRelation(captor.capture());
        assertThat(captor.getValue().getRelationId()).isEqualTo(5001L);
        assertThat(captor.getValue().getPostId()).isEqualTo(3001L);
        assertThat(captor.getValue().getPrimaryFlag()).isTrue();
        assertThat(captor.getValue().getLeaderFlag()).isTrue();
    }

    @Test
    void preservesExistingNonRootPrimaryOrg() {
        TenantMemberEntity admin = adminMember(1001L, 300L);
        when(tenantMemberMapper.selectList(any(Wrapper.class))).thenReturn(List.of(admin));
        when(tenantMemberProvider.listOrgRelations(2L, 200L)).thenReturn(List.of());

        provisioner.provision(new TenantProvisionCommand(2L, "company_a", "A公司"));

        verify(tenantMemberProvider).listOrgRelations(2L, 200L);
        verify(tenantMemberProvider, never()).addOrgRelation(any());
        verify(tenantMemberProvider, never()).updateOrgRelation(any());
        verify(roleBindingAdapter).ensureSubjectRoleBinding(any(SubjectRoleBindingCommand.class));
    }

    private TenantMemberEntity adminMember(Long memberId, Long primaryOrgId) {
        TenantMemberEntity member = new TenantMemberEntity();
        member.setId(memberId);
        member.setTenantId("2");
        member.setMemberType("INSTITUTION_ADMIN");
        member.setStatus(1);
        member.setPrimaryOrgId(primaryOrgId);
        return member;
    }
}
