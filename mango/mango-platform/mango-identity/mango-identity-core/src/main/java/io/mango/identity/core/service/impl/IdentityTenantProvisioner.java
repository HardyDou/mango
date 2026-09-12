package io.mango.identity.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.command.SubjectRoleBindingCommand;
import io.mango.authorization.api.query.RoleLookupQuery;
import io.mango.identity.core.adapter.AuthorizationRoleBindingAdapter;
import io.mango.identity.core.entity.IdentityUserEntity;
import io.mango.identity.core.entity.TenantMemberEntity;
import io.mango.identity.core.entity.TenantMemberLifecycleLogEntity;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.identity.core.mapper.TenantMemberLifecycleLogMapper;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.command.AddTenantMemberOrgCommand;
import io.mango.identity.api.command.UpdateTenantMemberOrgCommand;
import io.mango.identity.api.vo.TenantMemberOrgRelationVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.org.api.OrgReferenceProvider;
import io.mango.system.api.tenant.TenantDependencyChecker;
import io.mango.system.api.tenant.TenantProvisionCommand;
import io.mango.system.api.tenant.TenantProvisioner;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 身份模块租户初始化。
 */
@Component
@Order(300)
public class IdentityTenantProvisioner implements TenantProvisioner, TenantDependencyChecker {

    private static final String DEFAULT_APP_CODE = "internal-admin";
    private static final String DEFAULT_REALM = "INTERNAL";
    private static final String DEFAULT_ACTOR_TYPE = "INTERNAL_USER";
    private static final String DEFAULT_PARTY_TYPE = "INTERNAL_ORG";
    private static final String TENANT_ADMIN_ROLE = "ROLE_ADMIN";

    private final IdentityUserMapper identityUserMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final TenantMemberLifecycleLogMapper tenantMemberLifecycleLogMapper;
    private final AuthorizationRoleBindingAdapter roleBindingAdapter;
    private final ObjectProvider<TenantMemberProvider> tenantMemberProvider;
    private final ObjectProvider<OrgReferenceProvider> orgReferenceProvider;

    public IdentityTenantProvisioner(IdentityUserMapper identityUserMapper,
                                     TenantMemberMapper tenantMemberMapper,
                                     TenantMemberLifecycleLogMapper tenantMemberLifecycleLogMapper,
                                     AuthorizationRoleBindingAdapter roleBindingAdapter,
                                     ObjectProvider<TenantMemberProvider> tenantMemberProvider,
                                     ObjectProvider<OrgReferenceProvider> orgReferenceProvider) {
        this.identityUserMapper = identityUserMapper;
        this.tenantMemberMapper = tenantMemberMapper;
        this.tenantMemberLifecycleLogMapper = tenantMemberLifecycleLogMapper;
        this.roleBindingAdapter = roleBindingAdapter;
        this.tenantMemberProvider = tenantMemberProvider;
        this.orgReferenceProvider = orgReferenceProvider;
    }

    @Override
    public void provision(TenantProvisionCommand context) {
        Long creatorUserId = MangoContextHolder.userId();
        if (creatorUserId != null) {
            IdentityUserEntity creator = identityUserMapper.selectById(creatorUserId);
            if (creator != null) {
                ensureTenantAdminMember(context, creator);
            }
        }
        List<TenantMemberEntity> adminMembers = tenantMemberMapper.selectList(new LambdaQueryWrapper<TenantMemberEntity>()
                .eq(TenantMemberEntity::getTenantId, context.getTenantId())
                .eq(TenantMemberEntity::getMemberType, "INSTITUTION_ADMIN")
                .eq(TenantMemberEntity::getStatus, 1)
                .isNull(TenantMemberEntity::getLeftAt));
        ensureTenantAdminPrimaryOrg(context, adminMembers);
        Long roleId = findAdminRoleId(context.getTenantId());
        if (roleId == null) {
            return;
        }
        adminMembers.forEach(member -> ensureRoleBinding(context, member.getMemberId(), roleId));
    }

    private void ensureTenantAdminPrimaryOrg(TenantProvisionCommand context,
                                             List<TenantMemberEntity> adminMembers) {
        TenantMemberProvider memberProvider = tenantMemberProvider.getIfAvailable();
        OrgReferenceProvider orgProvider = orgReferenceProvider.getIfAvailable();
        if (memberProvider == null || orgProvider == null || adminMembers.isEmpty()) {
            return;
        }
        Long rootOrgId = orgProvider.resolveRootOrgId(context.getTenantId());
        if (rootOrgId == null) {
            return;
        }
        List<TenantMemberOrgRelationVO> rootRelations = memberProvider.listOrgRelations(
                context.getTenantId(), rootOrgId);
        adminMembers.forEach(member -> {
            if (member.getPrimaryOrgId() != null) {
                return;
            }
            TenantMemberOrgRelationVO relation = rootRelations.stream()
                    .filter(item -> member.getMemberId().equals(item.getMemberId()))
                    .findFirst()
                    .orElse(null);
            if (relation == null) {
                AddTenantMemberOrgCommand command = new AddTenantMemberOrgCommand();
                command.setTenantId(context.getTenantId());
                command.setMemberId(member.getMemberId());
                command.setOrgId(rootOrgId);
                command.setPrimaryFlag(true);
                command.setLeaderFlag(false);
                command.setOperatorUserId(MangoContextHolder.userId());
                memberProvider.addOrgRelation(command);
                return;
            }
            UpdateTenantMemberOrgCommand command = new UpdateTenantMemberOrgCommand();
            command.setRelationId(relation.getRelationId());
            command.setPostId(relation.getPostId());
            command.setPrimaryFlag(true);
            command.setLeaderFlag(Boolean.TRUE.equals(relation.getLeaderFlag()));
            command.setOperatorUserId(MangoContextHolder.userId());
            memberProvider.updateOrgRelation(command);
        });
    }

    @Override
    public Optional<String> check(Long tenantId) {
        Long memberCount = tenantMemberMapper.selectCount(new LambdaQueryWrapper<TenantMemberEntity>()
                .eq(TenantMemberEntity::getTenantId, tenantId));
        if (memberCount != null && memberCount > 0) {
            return Optional.of("机构已有成员数据，不能直接删除");
        }
        return Optional.empty();
    }

    private TenantMemberEntity ensureTenantAdminMember(TenantProvisionCommand context, IdentityUserEntity user) {
        TenantMemberEntity member = tenantMemberMapper.selectOne(new LambdaQueryWrapper<TenantMemberEntity>()
                .eq(TenantMemberEntity::getTenantId, context.getTenantId())
                .eq(TenantMemberEntity::getUserId, user.getUserId())
                .last("LIMIT 1"));
        if (member != null) {
            return member;
        }
        member = new TenantMemberEntity();
        member.setTenantId(String.valueOf(context.getTenantId()));
        member.setUserId(user.getUserId());
        member.setMemberNo("ADMIN-" + context.getTenantId() + "-" + user.getUserId());
        member.setDisplayName(firstText(user.getNickname(), user.getUsername()));
        member.setMemberType("INSTITUTION_ADMIN");
        member.setStatus(1);
        member.setJoinedAt(LocalDateTime.now());
        member.setRemark(context.getTenantName() + " 机构创建者");
        tenantMemberMapper.insert(member);
        TenantMemberLifecycleLogEntity event = new TenantMemberLifecycleLogEntity();
        event.setTenantId(member.getTenantId());
        event.setUserId(member.getUserId());
        event.setMemberId(member.getMemberId());
        event.setEventType("CREATED");
        event.setOperatorUserId(MangoContextHolder.userId());
        event.setOccurredAt(member.getJoinedAt());
        tenantMemberLifecycleLogMapper.insert(event);
        return member;
    }

    private Long findAdminRoleId(Long tenantId) {
        RoleLookupQuery query = new RoleLookupQuery();
        query.setTenantId(tenantId);
        query.setAppCode(DEFAULT_APP_CODE);
        query.setRealm(DEFAULT_REALM);
        query.setActorType(DEFAULT_ACTOR_TYPE);
        query.setRoleCode(TENANT_ADMIN_ROLE);
        return roleBindingAdapter.findRoleId(query);
    }

    private void ensureRoleBinding(TenantProvisionCommand context, Long memberId, Long roleId) {
        SubjectRoleBindingCommand command = new SubjectRoleBindingCommand();
        command.setTenantId(context.getTenantId());
        command.setSubjectType(AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER);
        command.setSubjectId(memberId);
        command.setRoleId(roleId);
        command.setAppCode(DEFAULT_APP_CODE);
        command.setRealm(DEFAULT_REALM);
        command.setActorType(DEFAULT_ACTOR_TYPE);
        command.setPartyType(DEFAULT_PARTY_TYPE);
        command.setPartyId(context.getTenantId());
        roleBindingAdapter.ensureSubjectRoleBinding(command);
    }

    private String firstText(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim();
        }
        return fallback;
    }
}
