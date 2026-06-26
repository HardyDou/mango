package io.mango.identity.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.entity.TenantMember;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Resource handler for demo and bootstrap identity users.
 */
@Component
@RequiredArgsConstructor
public class IdentityUserResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "identity_user";
    private static final String DEFAULT_REALM = "INTERNAL";
    private static final String DEFAULT_ACTOR_TYPE = "INTERNAL_USER";
    private static final String DEFAULT_PARTY_TYPE = "INTERNAL_ORG";
    private static final String DEFAULT_MEMBER_TYPE = "EMPLOYEE";

    private final IdentityUserMapper userMapper;
    private final TenantMemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final ResourceFieldReader fields = new ResourceFieldReader(ResourceTypes.IDENTITY_USER);

    @Override
    public String resourceType() {
        return ResourceTypes.IDENTITY_USER;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("tenantId")
                .requiredField("username")
                .fieldDescription("password", "明文初始密码，仅用于 demo/bootstrap；handler 会加密保存。")
                .fieldDescription("memberNo", "租户成员编号；未配置时使用 USER-{userId}。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        IdentityUser user = findUser(resource);
        LocalDateTime now = LocalDateTime.now();
        if (user == null) {
            user = new IdentityUser();
            user.setUsername(fields.requiredString(resource, "username"));
            user.setRealm(fields.stringField(resource, "realm", DEFAULT_REALM));
            user.setActorType(fields.stringField(resource, "actorType", DEFAULT_ACTOR_TYPE));
            user.setCreateTime(now);
        }
        applyUserFields(resource, user, now);
        if (user.getUserId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
        TenantMember member = upsertMember(resource, user, now);
        return ResourceSyncResult.of(user.getUserId(), TARGET_TABLE,
                "Identity user synced: " + user.getUsername() + ", memberId=" + member.getMemberId());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        IdentityUser user = findByTargetOrBusinessKey(resource);
        boolean changed = false;
        if (user != null && !Integer.valueOf(0).equals(user.getStatus())) {
            user.setStatus(0);
            user.setUpdateTime(LocalDateTime.now());
            changed = userMapper.updateById(user) > 0;
        }
        TenantMember member = user == null ? null : findMember(fields.requiredLong(resource, "tenantId"), user.getUserId());
        if (member != null && !Integer.valueOf(0).equals(member.getStatus())) {
            member.setStatus(0);
            changed = memberMapper.updateById(member) > 0 || changed;
        }
        return ResourceSyncResult.of(user == null ? null : user.getUserId(), TARGET_TABLE,
                "Identity user disabled: changed=" + changed);
    }

    private void applyUserFields(ResourceDeclaration resource, IdentityUser user, LocalDateTime now) {
        String password = fields.stringField(resource, "password");
        if (StringUtils.hasText(password)) {
            user.setPassword(passwordEncoder.encode(password.trim()));
        }
        user.setNickname(fields.stringField(resource, "nickname", user.getUsername()));
        user.setPartyType(fields.stringField(resource, "partyType", DEFAULT_PARTY_TYPE));
        user.setPartyId(fields.longField(resource, "partyId"));
        user.setEmail(fields.stringField(resource, "email"));
        user.setPhone(fields.stringField(resource, "phone"));
        user.setAvatar(fields.stringField(resource, "avatar"));
        user.setStatus(statusValue(resource));
        user.setTenantId(String.valueOf(fields.requiredLong(resource, "tenantId")));
        user.setRemark(fields.stringField(resource, "remark"));
        user.setUpdateTime(now);
    }

    private TenantMember upsertMember(ResourceDeclaration resource, IdentityUser user, LocalDateTime now) {
        Long tenantId = fields.requiredLong(resource, "tenantId");
        TenantMember member = findMember(tenantId, user.getUserId());
        if (member == null) {
            member = new TenantMember();
            member.setTenantId(tenantId);
            member.setUserId(user.getUserId());
            member.setMemberNo(fields.stringField(resource, "memberNo", "USER-" + user.getUserId()));
            member.setJoinedAt(now);
        }
        member.setDisplayName(fields.stringField(resource, "displayName",
                fields.stringField(resource, "nickname", user.getUsername())));
        member.setMemberType(fields.stringField(resource, "memberType", DEFAULT_MEMBER_TYPE));
        member.setStatus(statusValue(resource));
        member.setRemark(fields.stringField(resource, "remark"));
        if (member.getMemberId() == null) {
            memberMapper.insert(member);
        } else {
            memberMapper.updateById(member);
        }
        return member;
    }

    private IdentityUser findUser(ResourceDeclaration resource) {
        Long targetId = fields.longField(resource, "targetId");
        if (targetId != null) {
            IdentityUser user = userMapper.selectById(targetId);
            if (user != null) {
                return user;
            }
        }
        return findByBusinessKey(resource);
    }

    private IdentityUser findByTargetOrBusinessKey(ResourceDeclaration resource) {
        return findUser(resource);
    }

    private IdentityUser findByBusinessKey(ResourceDeclaration resource) {
        return userMapper.selectOne(new LambdaQueryWrapper<IdentityUser>()
                .eq(IdentityUser::getUsername, fields.requiredString(resource, "username"))
                .eq(IdentityUser::getRealm, fields.stringField(resource, "realm", DEFAULT_REALM))
                .eq(IdentityUser::getActorType, fields.stringField(resource, "actorType", DEFAULT_ACTOR_TYPE))
                .last("LIMIT 1"));
    }

    private TenantMember findMember(Long tenantId, Long userId) {
        return memberMapper.selectOne(new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getTenantId, tenantId)
                .eq(TenantMember::getUserId, userId)
                .isNull(TenantMember::getLeftAt)
                .last("LIMIT 1"));
    }

    private Integer statusValue(ResourceDeclaration resource) {
        Integer status = fields.intField(resource, "status", null);
        if (status != null) {
            return status;
        }
        return resource.getStatus() == ResourceStatus.DISABLED ? 0 : 1;
    }
}
