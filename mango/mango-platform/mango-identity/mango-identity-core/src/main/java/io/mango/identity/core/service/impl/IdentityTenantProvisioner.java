package io.mango.identity.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.core.entity.Role;
import io.mango.authorization.core.entity.SubjectRoleBinding;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.entity.TenantMember;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.system.api.tenant.TenantDependencyChecker;
import io.mango.system.api.tenant.TenantProvisionContext;
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
    private final RoleMapper roleMapper;
    private final SubjectRoleBindingMapper subjectRoleBindingMapper;

    @Override
    public void provision(TenantProvisionContext context) {
        Long creatorUserId = MangoContextHolder.userId();
        if (creatorUserId == null) {
            return;
        }
        IdentityUser creator = identityUserMapper.selectById(creatorUserId);
        if (creator == null) {
            return;
        }
        TenantMember member = ensureTenantAdminMember(context, creator);
        Role role = findAdminRole(context.tenantId());
        if (role != null) {
            ensureRoleBinding(context, member.getMemberId(), role.getRoleId());
        }
    }

    @Override
    public Optional<String> check(Long tenantId) {
        Long memberCount = tenantMemberMapper.selectCount(new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getTenantId, tenantId));
        if (memberCount != null && memberCount > 0) {
            return Optional.of("机构已有成员数据，不能直接删除");
        }
        return Optional.empty();
    }

    private TenantMember ensureTenantAdminMember(TenantProvisionContext context, IdentityUser user) {
        TenantMember member = tenantMemberMapper.selectOne(new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getTenantId, context.tenantId())
                .eq(TenantMember::getUserId, user.getUserId())
                .last("LIMIT 1"));
        if (member != null) {
            return member;
        }
        member = new TenantMember();
        member.setTenantId(context.tenantId());
        member.setUserId(user.getUserId());
        member.setMemberNo("ADMIN-" + context.tenantId() + "-" + user.getUserId());
        member.setDisplayName(firstText(user.getNickname(), user.getUsername()));
        member.setMemberType("INSTITUTION_ADMIN");
        member.setStatus(1);
        member.setJoinedAt(LocalDateTime.now());
        member.setRemark(context.tenantName() + " 机构创建者");
        tenantMemberMapper.insert(member);
        return member;
    }

    private Role findAdminRole(Long tenantId) {
        return roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getTenantId, tenantId)
                .eq(Role::getAppCode, DEFAULT_APP_CODE)
                .eq(Role::getRoleCode, TENANT_ADMIN_ROLE)
                .last("LIMIT 1"));
    }

    private void ensureRoleBinding(TenantProvisionContext context, Long memberId, Long roleId) {
        Long count = subjectRoleBindingMapper.selectCount(new LambdaQueryWrapper<SubjectRoleBinding>()
                .eq(SubjectRoleBinding::getTenantId, context.tenantId())
                .eq(SubjectRoleBinding::getSubjectType, AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER)
                .eq(SubjectRoleBinding::getSubjectId, memberId)
                .eq(SubjectRoleBinding::getRoleId, roleId)
                .eq(SubjectRoleBinding::getAppCode, DEFAULT_APP_CODE)
                .eq(SubjectRoleBinding::getRealm, DEFAULT_REALM)
                .eq(SubjectRoleBinding::getActorType, DEFAULT_ACTOR_TYPE)
                .eq(SubjectRoleBinding::getPartyType, DEFAULT_PARTY_TYPE)
                .eq(SubjectRoleBinding::getPartyId, context.tenantId()));
        if (count != null && count > 0) {
            return;
        }
        SubjectRoleBinding binding = new SubjectRoleBinding();
        binding.setTenantId(context.tenantId());
        binding.setSubjectType(AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER);
        binding.setSubjectId(memberId);
        binding.setRoleId(roleId);
        binding.setAppCode(DEFAULT_APP_CODE);
        binding.setRealm(DEFAULT_REALM);
        binding.setActorType(DEFAULT_ACTOR_TYPE);
        binding.setPartyType(DEFAULT_PARTY_TYPE);
        binding.setPartyId(context.tenantId());
        subjectRoleBindingMapper.insert(binding);
    }

    private String firstText(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred.trim() : fallback;
    }
}
