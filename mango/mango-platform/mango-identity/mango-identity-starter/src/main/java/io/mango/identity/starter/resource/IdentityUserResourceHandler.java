package io.mango.identity.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.identity.core.entity.IdentityUserEntity;
import io.mango.identity.core.entity.TenantMemberEntity;
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
                .fieldDescription("memberId", "固定租户成员 ID；用于被授权等资源稳定引用。")
                .fieldDescription("memberNo", "租户成员编号；未配置时使用 USER-{userId}。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        IdentityUserEntity user = findUser(resource);
        LocalDateTime now = LocalDateTime.now();
        if (user == null) {
            user = new IdentityUserEntity();
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
        TenantMemberEntity member = upsertMember(resource, user, now);
        return ResourceSyncResult.of(user.getUserId(), TARGET_TABLE,
                "Identity user synced: " + user.getUsername() + ", memberId=" + member.getMemberId());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        IdentityUserEntity user = findByTargetOrBusinessKey(resource);
        boolean changed = false;
        if (user != null && !Integer.valueOf(0).equals(user.getStatus())) {
            user.setStatus(0);
            user.setUpdateTime(LocalDateTime.now());
            changed = userMapper.updateById(user) > 0;
        }
        TenantMemberEntity member = user == null ? null : findMember(fields.requiredLong(resource, "tenantId"), user.getUserId());
        if (member != null && !Integer.valueOf(0).equals(member.getStatus())) {
            member.setStatus(0);
            changed = memberMapper.updateById(member) > 0 || changed;
        }
        return ResourceSyncResult.of(user == null ? null : user.getUserId(), TARGET_TABLE,
                "Identity user disabled: changed=" + changed);
    }

    private void applyUserFields(ResourceDeclaration resource, IdentityUserEntity user, LocalDateTime now) {
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
        if (user.getTenantId() == null) {
            user.setTenantId(String.valueOf(fields.requiredLong(resource, "tenantId")));
        }
        user.setRemark(fields.stringField(resource, "remark"));
        user.setUpdateTime(now);
    }

    private TenantMemberEntity upsertMember(ResourceDeclaration resource, IdentityUserEntity user, LocalDateTime now) {
        Long tenantId = fields.requiredLong(resource, "tenantId");
        TenantMemberEntity member = findMember(tenantId, user.getUserId());
        boolean newMember = member == null;
        if (newMember) {
            member = new TenantMemberEntity();
            member.setMemberId(fields.longField(resource, "memberId"));
            member.setTenantId(String.valueOf(tenantId));
            member.setUserId(user.getUserId());
            member.setMemberNo(fields.stringField(resource, "memberNo", "USER-" + user.getUserId()));
            member.setJoinedAt(now);
        }
        member.setDisplayName(fields.stringField(resource, "displayName",
                fields.stringField(resource, "nickname", user.getUsername())));
        member.setMemberType(fields.stringField(resource, "memberType", DEFAULT_MEMBER_TYPE));
        member.setStatus(statusValue(resource));
        member.setRemark(fields.stringField(resource, "remark"));
        if (newMember) {
            memberMapper.insert(member);
        } else {
            memberMapper.updateById(member);
        }
        return member;
    }

    private IdentityUserEntity findUser(ResourceDeclaration resource) {
        Long targetId = fields.longField(resource, "targetId");
        if (targetId != null) {
            IdentityUserEntity user = userMapper.selectById(targetId);
            if (user != null) {
                return user;
            }
        }
        return findByBusinessKey(resource);
    }

    private IdentityUserEntity findByTargetOrBusinessKey(ResourceDeclaration resource) {
        return findUser(resource);
    }

    private IdentityUserEntity findByBusinessKey(ResourceDeclaration resource) {
        return userMapper.selectOne(new LambdaQueryWrapper<IdentityUserEntity>()
                .eq(IdentityUserEntity::getUsername, fields.requiredString(resource, "username"))
                .eq(IdentityUserEntity::getRealm, fields.stringField(resource, "realm", DEFAULT_REALM))
                .eq(IdentityUserEntity::getActorType, fields.stringField(resource, "actorType", DEFAULT_ACTOR_TYPE))
                .last("LIMIT 1"));
    }

    private TenantMemberEntity findMember(Long tenantId, Long userId) {
        return memberMapper.selectOne(new LambdaQueryWrapper<TenantMemberEntity>()
                .eq(TenantMemberEntity::getTenantId, tenantId)
                .eq(TenantMemberEntity::getUserId, userId)
                .isNull(TenantMemberEntity::getLeftAt)
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
