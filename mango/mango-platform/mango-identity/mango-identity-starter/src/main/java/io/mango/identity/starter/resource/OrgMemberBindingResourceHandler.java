package io.mango.identity.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.identity.core.entity.IdentityUserEntity;
import io.mango.identity.core.entity.TenantMemberEntity;
import io.mango.identity.core.entity.TenantMemberOrgEntity;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.identity.core.mapper.TenantMemberOrgMapper;
import io.mango.org.api.OrgReferenceProvider;
import io.mango.resource.support.PortableResourceIds;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceHandlerSpec;
import io.mango.resource.support.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

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
    private final OrgReferenceProvider orgReferenceProvider;
    private final ResourceFieldReader fields = new ResourceFieldReader(ResourceTypes.ORG_MEMBER_BINDING);

    @Override
    public String resourceType() {
        return ResourceTypes.ORG_MEMBER_BINDING;
    }

    @Override
    public String executionTenantField() {
        return "tenantId";
    }

    @Override
    public List<String> dependsOnResourceTypes() {
        return List.of(ResourceTypes.IDENTITY_USER, ResourceTypes.ORG_UNIT, ResourceTypes.ORG_POST);
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("tenantId")
                .requiredField("orgCode")
                .fieldDescription("targetId", "可选稳定关系 ID；未填写时按租户、成员和组织编码稳定派生。")
                .fieldDescription("memberId", "成员 ID。memberId、memberNo、username 三选一。")
                .fieldDescription("memberNo", "成员编号。")
                .fieldDescription("username", "用户名，用于解析租户成员。")
                .fieldDescription("postCode", "岗位编码。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        Long tenantId = fields.requiredLong(resource, "tenantId");
        TenantMemberEntity member = requiredMember(resource, tenantId);
        String orgCode = fields.requiredString(resource, "orgCode");
        Long orgId = requiredOrgId(tenantId, orgCode);
        Long postId = optionalPostId(resource, tenantId);
        TenantMemberOrgEntity relation = findRelation(tenantId, member.getMemberId(), orgId);
        boolean newRelation = relation == null;
        LocalDateTime now = LocalDateTime.now();
        if (newRelation) {
            relation = newRelation(resource, tenantId, member.getMemberId(), orgId, orgCode, now);
        }
        relation.setPostId(postId);
        relation.setPrimaryFlag(Boolean.TRUE.equals(fields.boolField(resource, "primaryOrg", false)) ? 1 : 0);
        relation.setLeaderFlag(Boolean.TRUE.equals(fields.boolField(resource, "leader", false)) ? 1 : 0);
        relation.setUpdatedAt(now);
        if (newRelation) {
            memberOrgMapper.insert(relation);
        } else {
            memberOrgMapper.updateById(relation);
        }
        updateMemberPrimary(member, relation);
        return ResourceSyncResult.of(relation.getId(), TARGET_TABLE,
                "Org member binding synced: memberId=" + member.getMemberId() + ", orgCode="
                        + orgCode);
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        Long tenantId = fields.requiredLong(resource, "tenantId");
        TenantMemberEntity member = requiredMember(resource, tenantId);
        Long orgId = requiredOrgId(tenantId, fields.requiredString(resource, "orgCode"));
        TenantMemberOrgEntity relation = findRelation(tenantId, member.getMemberId(), orgId);
        boolean changed = relation != null && memberOrgMapper.deleteById(relation.getId()) > 0;
        return ResourceSyncResult.of(relation == null ? null : relation.getId(), TARGET_TABLE,
                "Org member binding disabled: changed=" + changed);
    }

    private TenantMemberEntity requiredMember(ResourceDeclaration resource, Long tenantId) {
        Long memberId = fields.longField(resource, "memberId");
        if (memberId != null) {
            TenantMemberEntity member = memberMapper.selectById(memberId);
            if (member != null && String.valueOf(tenantId).equals(member.getTenantId())) {
                return member;
            }
        }
        String memberNo = fields.stringField(resource, "memberNo");
        if (StringUtils.hasText(memberNo)) {
            TenantMemberEntity member = memberMapper.selectOne(new LambdaQueryWrapper<TenantMemberEntity>()
                    .eq(TenantMemberEntity::getTenantId, tenantId)
                    .eq(TenantMemberEntity::getMemberNo, memberNo.trim())
                    .isNull(TenantMemberEntity::getLeftAt)
                    .last("LIMIT 1"));
            if (member != null) {
                return member;
            }
        }
        String username = fields.stringField(resource, "username");
        if (StringUtils.hasText(username)) {
            IdentityUserEntity user = userMapper.selectOne(new LambdaQueryWrapper<IdentityUserEntity>()
                    .eq(IdentityUserEntity::getUsername, username.trim())
                    .last("LIMIT 1"));
            if (user != null) {
                TenantMemberEntity member = memberMapper.selectOne(new LambdaQueryWrapper<TenantMemberEntity>()
                        .eq(TenantMemberEntity::getTenantId, tenantId)
                        .eq(TenantMemberEntity::getUserId, user.getUserId())
                        .isNull(TenantMemberEntity::getLeftAt)
                        .last("LIMIT 1"));
                if (member != null) {
                    return member;
                }
            }
        }
        throw new IllegalStateException("ORG_MEMBER_BINDING referenced member does not exist");
    }

    private Long requiredOrgId(Long tenantId, String orgCode) {
        Long orgId = orgReferenceProvider.resolveOrgId(tenantId, orgCode);
        if (orgId == null) {
            throw new IllegalStateException("ORG_MEMBER_BINDING referenced org does not exist: "
                    + orgCode);
        }
        return orgId;
    }

    private TenantMemberOrgEntity newRelation(ResourceDeclaration resource, Long tenantId, Long memberId,
                                               Long orgId, String orgCode, LocalDateTime createdAt) {
        long relationId = PortableResourceIds.declaredOrStable(
                fields.longField(resource, "targetId"), TARGET_TABLE, tenantId, memberId, orgCode);
        TenantMemberOrgEntity occupied = memberOrgMapper.selectById(relationId);
        if (occupied != null) {
            throw new IllegalStateException("ORG_MEMBER_BINDING portable relation ID collision: id=" + relationId);
        }
        TenantMemberOrgEntity relation = new TenantMemberOrgEntity();
        relation.setId(relationId);
        relation.setTenantId(String.valueOf(tenantId));
        relation.setMemberId(memberId);
        relation.setOrgId(orgId);
        relation.setCreatedAt(createdAt);
        return relation;
    }

    private Long optionalPostId(ResourceDeclaration resource, Long tenantId) {
        String postCode = fields.stringField(resource, "postCode");
        if (!StringUtils.hasText(postCode)) {
            return null;
        }
        Long postId = orgReferenceProvider.resolvePostId(tenantId, postCode);
        if (postId == null) {
            throw new IllegalStateException("ORG_MEMBER_BINDING referenced post does not exist: " + postCode);
        }
        return postId;
    }

    private TenantMemberOrgEntity findRelation(Long tenantId, Long memberId, Long orgId) {
        return memberOrgMapper.selectOne(new LambdaQueryWrapper<TenantMemberOrgEntity>()
                .eq(TenantMemberOrgEntity::getTenantId, tenantId)
                .eq(TenantMemberOrgEntity::getMemberId, memberId)
                .eq(TenantMemberOrgEntity::getOrgId, orgId)
                .last("LIMIT 1"));
    }

    private void updateMemberPrimary(TenantMemberEntity member, TenantMemberOrgEntity relation) {
        if (!Integer.valueOf(1).equals(relation.getPrimaryFlag())) {
            return;
        }
        member.setPrimaryOrgId(relation.getOrgId());
        member.setPrimaryPostId(relation.getPostId());
        memberMapper.updateById(member);
    }
}
