package io.mango.identity.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.identity.core.entity.IdentityUserEntity;
import io.mango.identity.core.entity.TenantMemberEntity;
import io.mango.identity.core.entity.TenantMemberLifecycleLogEntity;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.identity.core.mapper.TenantMemberLifecycleLogMapper;
import io.mango.resource.support.PortableResourceIds;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceHandlerSpec;
import io.mango.resource.support.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

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
    private static final String MEMBER_LIFECYCLE_TABLE = "tenant_member_lifecycle_log";
    private static final String MEMBER_CREATED_EVENT = "CREATED";

    private final IdentityUserMapper userMapper;
    private final TenantMemberMapper memberMapper;
    private final TenantMemberLifecycleLogMapper lifecycleLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final ResourceFieldReader fields = new ResourceFieldReader(ResourceTypes.IDENTITY_USER);

    @Override
    public String resourceType() {
        return ResourceTypes.IDENTITY_USER;
    }

    @Override
    public String executionTenantField() {
        return "tenantId";
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("tenantId")
                .requiredField("username")
                .requiredField("memberId")
                .requiredField("initializedAt")
                .fieldDescription("password", "明文初始密码，仅用于 demo 或运行时初始化；handler 会加密保存。")
                .fieldDescription("encodedPassword", "PasswordEncoder 已编码密码；正式可移植基线使用该字段保证构建结果确定。")
                .fieldDescription("memberId", "固定租户成员 ID；用于被授权等资源稳定引用。")
                .fieldDescription("memberNo", "租户成员编号；未配置时使用 USER-{userId}。")
                .fieldDescription("initializedAt", "固定初始化时间；用于可移植基线中的用户、成员和成员创建事件。")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        IdentityUserEntity user = findUser(resource);
        boolean newUser = user == null;
        LocalDateTime initializationTime = fields.requiredDateTime(resource, "initializedAt");
        Long tenantId = fields.requiredLong(resource, "tenantId");
        Long memberId = fields.requiredLong(resource, "memberId");
        Long expectedUserId = newUser ? fields.longField(resource, "targetId") : user.getUserId();
        validateLifecycleEventIdentity(tenantId, memberId, expectedUserId);
        if (newUser) {
            user = new IdentityUserEntity();
            user.setUserId(fields.longField(resource, "targetId"));
            user.setUsername(fields.requiredString(resource, "username"));
            user.setRealm(fields.stringField(resource, "realm", DEFAULT_REALM));
            user.setActorType(fields.stringField(resource, "actorType", DEFAULT_ACTOR_TYPE));
            user.setCreateTime(initializationTime);
            user.setCreatedAt(initializationTime);
        }
        applyUserFields(resource, user, initializationTime);
        if (newUser) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
        TenantMemberEntity member = upsertMember(resource, user, initializationTime);
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
        String encodedPassword = fields.stringField(resource, "encodedPassword");
        if (StringUtils.hasText(password) && StringUtils.hasText(encodedPassword)) {
            throw new IllegalStateException(resourceType()
                    + " password and encodedPassword cannot both be declared: " + resource.getId());
        }
        if (StringUtils.hasText(encodedPassword)) {
            user.setPassword(encodedPassword.trim());
        } else if (StringUtils.hasText(password)) {
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
        user.setUpdatedAt(now);
    }

    private TenantMemberEntity upsertMember(ResourceDeclaration resource, IdentityUserEntity user, LocalDateTime now) {
        Long tenantId = fields.requiredLong(resource, "tenantId");
        TenantMemberEntity member = findMember(tenantId, user.getUserId());
        boolean newMember = member == null;
        if (newMember) {
            member = new TenantMemberEntity();
            member.setMemberId(fields.requiredLong(resource, "memberId"));
            member.setTenantId(String.valueOf(tenantId));
            member.setUserId(user.getUserId());
            member.setMemberNo(fields.stringField(resource, "memberNo", "USER-" + user.getUserId()));
            member.setJoinedAt(now);
            member.setCreatedAt(now);
        }
        member.setDisplayName(fields.stringField(resource, "displayName",
                fields.stringField(resource, "nickname", user.getUsername())));
        member.setMemberType(fields.stringField(resource, "memberType", DEFAULT_MEMBER_TYPE));
        member.setStatus(statusValue(resource));
        member.setRemark(fields.stringField(resource, "remark"));
        member.setUpdatedAt(now);
        if (newMember) {
            TenantMemberLifecycleLogEntity event = lifecycleCreatedEvent(member, now);
            TenantMemberLifecycleLogEntity existingEvent = lifecycleLogMapper.selectById(event.getId());
            if (existingEvent != null && !sameLifecycleEvent(existingEvent, event)) {
                throw new IllegalStateException("Identity portable lifecycle event ID collision: id=" + event.getId());
            }
            memberMapper.insert(member);
            if (existingEvent == null) {
                lifecycleLogMapper.insert(event);
            }
        } else {
            memberMapper.updateById(member);
        }
        return member;
    }

    private TenantMemberLifecycleLogEntity lifecycleCreatedEvent(TenantMemberEntity member, LocalDateTime occurredAt) {
        TenantMemberLifecycleLogEntity event = new TenantMemberLifecycleLogEntity();
        event.setId(PortableResourceIds.stable(MEMBER_LIFECYCLE_TABLE,
                member.getTenantId(), member.getMemberId(), MEMBER_CREATED_EVENT));
        event.setTenantId(member.getTenantId());
        event.setUserId(member.getUserId());
        event.setMemberId(member.getMemberId());
        event.setEventType(MEMBER_CREATED_EVENT);
        event.setOccurredAt(occurredAt);
        event.setCreatedAt(occurredAt);
        event.setUpdatedAt(occurredAt);
        return event;
    }

    private void validateLifecycleEventIdentity(Long tenantId, Long memberId, Long userId) {
        long eventId = PortableResourceIds.stable(
                MEMBER_LIFECYCLE_TABLE, tenantId, memberId, MEMBER_CREATED_EVENT);
        TenantMemberLifecycleLogEntity existing = lifecycleLogMapper.selectById(eventId);
        if (existing == null) {
            return;
        }
        boolean sameIdentity = Objects.equals(existing.getTenantId(), String.valueOf(tenantId))
                && Objects.equals(existing.getMemberId(), memberId)
                && Objects.equals(existing.getEventType(), MEMBER_CREATED_EVENT)
                && userId != null
                && Objects.equals(existing.getUserId(), userId);
        if (!sameIdentity) {
            throw new IllegalStateException("Identity portable lifecycle event ID collision: id=" + eventId);
        }
    }

    private boolean sameLifecycleEvent(
            TenantMemberLifecycleLogEntity existing,
            TenantMemberLifecycleLogEntity expected) {
        return Objects.equals(existing.getTenantId(), expected.getTenantId())
                && Objects.equals(existing.getUserId(), expected.getUserId())
                && Objects.equals(existing.getMemberId(), expected.getMemberId())
                && Objects.equals(existing.getEventType(), expected.getEventType());
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
