package io.mango.identity.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.command.SubjectRoleBindingCommand;
import io.mango.authorization.api.query.RoleLookupQuery;
import io.mango.identity.core.adapter.AuthorizationRoleBindingAdapter;
import io.mango.identity.core.entity.IdentityUserEntity;
import io.mango.identity.core.entity.TenantMemberEntity;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.system.api.tenant.TenantDependencyChecker;
import io.mango.system.api.tenant.TenantProvisionCommand;
import io.mango.system.api.tenant.TenantProvisioner;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 身份模块租户初始化。
 */
@Component
@Order(300)
@RequiredArgsConstructor
public class IdentityTenantProvisioner implements TenantProvisioner, TenantDependencyChecker {

    private static final String DEFAULT_APP_CODE = "internal-admin";
    private static final String DEFAULT_REALM = "INTERNAL";
    private static final String DEFAULT_ACTOR_TYPE = "INTERNAL_USER";
    private static final String DEFAULT_PARTY_TYPE = "INTERNAL_ORG";
    private static final String TENANT_ADMIN_ROLE = "ROLE_ADMIN";

    private final IdentityUserMapper identityUserMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final AuthorizationRoleBindingAdapter roleBindingAdapter;

    @Override
    public void provision(TenantProvisionCommand context) {
        Long creatorUserId = MangoContextHolder.userId();
        if (creatorUserId == null) {
            return;
        }
        IdentityUserEntity creator = identityUserMapper.selectById(creatorUserId);
        if (creator == null) {
            return;
        }
        TenantMemberEntity member = ensureTenantAdminMember(context, creator);
        Long roleId = findAdminRoleId(context.getTenantId());
        if (roleId != null) {
            ensureRoleBinding(context, member.getMemberId(), roleId);
        }
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
