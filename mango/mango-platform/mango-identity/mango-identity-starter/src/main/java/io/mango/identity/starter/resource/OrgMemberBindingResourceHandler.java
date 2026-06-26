package io.mango.identity.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.entity.TenantMember;
import io.mango.identity.core.entity.TenantMemberOrgEntity;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.identity.core.mapper.TenantMemberOrgMapper;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.core.entity.PostEntity;
import io.mango.org.core.mapper.PostMapper;
import io.mango.org.core.mapper.SysOrgMapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Resource handler for member organization and post bindings.
 */
@Component
@RequiredArgsConstructor
public class OrgMemberBindingResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "tenant_member_org";

    private final IdentityUserMapper userMapper;
    private final TenantMemberMapper memberMapper;
    private final TenantMemberOrgMapper memberOrgMapper;
    private final SysOrgMapper orgMapper;
    private final PostMapper postMapper;
    private final ResourceFieldReader fields = new ResourceFieldReader(ResourceTypes.ORG_MEMBER_BINDING);

    @Override
    public String resourceType() {
        return ResourceTypes.ORG_MEMBER_BINDING;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("tenantId")
                .requiredField("orgCode")
                .fieldDescription("memberId", "成员 ID。memberId、memberNo、username 三选一。")
                .fieldDescription("memberNo", "成员编号。")
                .fieldDescription("username", "用户名，用于解析租户成员。")
                .fieldDescription("postCode", "岗位编码。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        Long tenantId = fields.requiredLong(resource, "tenantId");
        TenantMember member = requiredMember(resource, tenantId);
        SysOrg org = requiredOrg(resource, tenantId);
        PostEntity post = optionalPost(resource, tenantId);
        TenantMemberOrgEntity relation = findRelation(tenantId, member.getMemberId(), org.getId());
        LocalDateTime now = LocalDateTime.now();
        if (relation == null) {
            relation = new TenantMemberOrgEntity();
            relation.setTenantId(tenantId);
            relation.setMemberId(member.getMemberId());
            relation.setOrgId(org.getId());
            relation.setCreatedAt(now);
        }
        relation.setPostId(post == null ? null : post.getId());
        relation.setPrimaryFlag(Boolean.TRUE.equals(fields.boolField(resource, "primaryOrg", false)) ? 1 : 0);
        relation.setLeaderFlag(Boolean.TRUE.equals(fields.boolField(resource, "leader", false)) ? 1 : 0);
        relation.setUpdatedAt(now);
        if (relation.getId() == null) {
            memberOrgMapper.insert(relation);
        } else {
            memberOrgMapper.updateById(relation);
        }
        updateMemberPrimary(member, relation);
        return ResourceSyncResult.of(relation.getId(), TARGET_TABLE,
                "Org member binding synced: memberId=" + member.getMemberId() + ", orgCode=" + org.getOrgCode());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        Long tenantId = fields.requiredLong(resource, "tenantId");
        TenantMember member = requiredMember(resource, tenantId);
        SysOrg org = requiredOrg(resource, tenantId);
        TenantMemberOrgEntity relation = findRelation(tenantId, member.getMemberId(), org.getId());
        boolean changed = relation != null && memberOrgMapper.deleteById(relation.getId()) > 0;
        return ResourceSyncResult.of(relation == null ? null : relation.getId(), TARGET_TABLE,
                "Org member binding disabled: changed=" + changed);
    }

    private TenantMember requiredMember(ResourceDeclaration resource, Long tenantId) {
        Long memberId = fields.longField(resource, "memberId");
        if (memberId != null) {
            TenantMember member = memberMapper.selectById(memberId);
            if (member != null && tenantId.equals(member.getTenantId())) {
                return member;
            }
        }
        String memberNo = fields.stringField(resource, "memberNo");
        if (StringUtils.hasText(memberNo)) {
            TenantMember member = memberMapper.selectOne(new LambdaQueryWrapper<TenantMember>()
                    .eq(TenantMember::getTenantId, tenantId)
                    .eq(TenantMember::getMemberNo, memberNo.trim())
                    .isNull(TenantMember::getLeftAt)
                    .last("LIMIT 1"));
            if (member != null) {
                return member;
            }
        }
        String username = fields.stringField(resource, "username");
        if (StringUtils.hasText(username)) {
            IdentityUser user = userMapper.selectOne(new LambdaQueryWrapper<IdentityUser>()
                    .eq(IdentityUser::getUsername, username.trim())
                    .last("LIMIT 1"));
            if (user != null) {
                TenantMember member = memberMapper.selectOne(new LambdaQueryWrapper<TenantMember>()
                        .eq(TenantMember::getTenantId, tenantId)
                        .eq(TenantMember::getUserId, user.getUserId())
                        .isNull(TenantMember::getLeftAt)
                        .last("LIMIT 1"));
                if (member != null) {
                    return member;
                }
            }
        }
        throw new IllegalStateException("ORG_MEMBER_BINDING referenced member does not exist");
    }

    private SysOrg requiredOrg(ResourceDeclaration resource, Long tenantId) {
        SysOrg org = orgMapper.selectOne(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getTenantId, tenantId)
                .eq(SysOrg::getOrgCode, fields.requiredString(resource, "orgCode"))
                .last("LIMIT 1"));
        if (org == null) {
            throw new IllegalStateException("ORG_MEMBER_BINDING referenced org does not exist: "
                    + fields.requiredString(resource, "orgCode"));
        }
        return org;
    }

    private PostEntity optionalPost(ResourceDeclaration resource, Long tenantId) {
        String postCode = fields.stringField(resource, "postCode");
        if (!StringUtils.hasText(postCode)) {
            return null;
        }
        PostEntity post = postMapper.selectOne(new LambdaQueryWrapper<PostEntity>()
                .eq(PostEntity::getTenantId, tenantId)
                .eq(PostEntity::getPostCode, postCode.trim())
                .last("LIMIT 1"));
        if (post == null) {
            throw new IllegalStateException("ORG_MEMBER_BINDING referenced post does not exist: " + postCode);
        }
        return post;
    }

    private TenantMemberOrgEntity findRelation(Long tenantId, Long memberId, Long orgId) {
        return memberOrgMapper.selectOne(new LambdaQueryWrapper<TenantMemberOrgEntity>()
                .eq(TenantMemberOrgEntity::getTenantId, tenantId)
                .eq(TenantMemberOrgEntity::getMemberId, memberId)
                .eq(TenantMemberOrgEntity::getOrgId, orgId)
                .last("LIMIT 1"));
    }

    private void updateMemberPrimary(TenantMember member, TenantMemberOrgEntity relation) {
        if (!Integer.valueOf(1).equals(relation.getPrimaryFlag())) {
            return;
        }
        member.setPrimaryOrgId(relation.getOrgId());
        member.setPrimaryPostId(relation.getPostId());
        memberMapper.updateById(member);
    }
}
