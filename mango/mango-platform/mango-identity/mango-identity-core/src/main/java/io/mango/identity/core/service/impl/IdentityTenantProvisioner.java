package io.mango.identity.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.RoleBindingApi;
import io.mango.authorization.api.command.SubjectRoleBindingCommand;
import io.mango.authorization.api.query.RoleLookupQuery;
import io.mango.common.result.R;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.entity.TenantMember;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.infra.context.api.MangoContextHolder;
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
    private final RoleBindingApi roleBindingApi;

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
        Long roleId = findAdminRoleId(context.tenantId());
        if (roleId != null) {
            ensureRoleBinding(context, member.getMemberId(), roleId);
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

    private Long findAdminRoleId(Long tenantId) {
        RoleLookupQuery query = new RoleLookupQuery();
        query.setTenantId(tenantId);
        query.setAppCode(DEFAULT_APP_CODE);
        query.setRoleCode(TENANT_ADMIN_ROLE);
        R<Long> response = roleBindingApi.findRoleId(query);
        return response != null && response.isSuccess() ? response.getData() : null;
    }

    private void ensureRoleBinding(TenantProvisionContext context, Long memberId, Long roleId) {
        SubjectRoleBindingCommand command = new SubjectRoleBindingCommand();
        command.setTenantId(context.tenantId());
        command.setSubjectType(AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER);
        command.setSubjectId(memberId);
        command.setRoleId(roleId);
        command.setAppCode(DEFAULT_APP_CODE);
        command.setRealm(DEFAULT_REALM);
        command.setActorType(DEFAULT_ACTOR_TYPE);
        command.setPartyType(DEFAULT_PARTY_TYPE);
        command.setPartyId(context.tenantId());
        roleBindingApi.ensureSubjectRoleBinding(command);
    }

    private String firstText(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred.trim() : fallback;
    }
}
