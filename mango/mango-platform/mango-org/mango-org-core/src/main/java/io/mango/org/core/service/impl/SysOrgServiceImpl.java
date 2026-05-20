package io.mango.org.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.entity.TenantMember;
import io.mango.identity.core.entity.TenantMemberOrgEntity;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.identity.core.mapper.TenantMemberOrgMapper;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.org.api.command.AddOrgMemberCommand;
import io.mango.org.api.command.CreateOrgCommand;
import io.mango.org.api.command.UpdateOrgMemberCommand;
import io.mango.org.api.command.UpdateOrgCommand;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.api.enums.PostCode;
import io.mango.org.api.vo.OrgMemberVO;
import io.mango.org.core.entity.PostEntity;
import io.mango.org.core.mapper.PostMapper;
import io.mango.org.core.service.ISysOrgService;
import io.mango.org.core.mapper.SysOrgMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

/**
 * Organization service implementation
 * <p>
 * Builds a complete organization tree. Organization data is small enough to
 * load as a tree for selector and management pages.
 *
 * @author Mango
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOrgServiceImpl implements ISysOrgService {

    private static final String DEPT_MANAGER_POST_CODE = "DEPT_MANAGER";
    private static final String ORG_MANAGER_POST_CODE = "ORG_MANAGER";
    private static final String TEAM_LEADER_POST_CODE = "TEAM_LEADER";

    private final SysOrgMapper orgMapper;
    private final PostMapper postMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final TenantMemberOrgMapper tenantMemberOrgMapper;
    private final IdentityUserMapper identityUserMapper;

    @Override
    public List<SysOrg> tree(Long parentId, Integer type) {
        return tree(parentId, type, false);
    }

    @Override
    public List<SysOrg> tree(Long parentId, Integer type, boolean includeDisabled) {
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(!includeDisabled, SysOrg::getOrgStatus, "1")
               .orderByAsc(SysOrg::getOrgSort)
               .orderByAsc(SysOrg::getId);

        List<SysOrg> orgs = orgMapper.selectList(wrapper);
        if (orgs == null || orgs.isEmpty()) {
            return List.of();
        }

        Map<Long, List<SysOrg>> childrenByParentId = orgs.stream()
                .collect(Collectors.groupingBy(org -> org.getPid() == null ? 0L : org.getPid()));

        Long rootParentId = parentId == null ? 0L : parentId;
        return childrenByParentId.getOrDefault(rootParentId, List.of()).stream()
                .map(org -> buildTreeNode(org, childrenByParentId, type))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<SysOrg> children(Long parentId) {
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOrg::getPid, parentId)
               .orderByAsc(SysOrg::getOrgSort)
               .orderByAsc(SysOrg::getId);

        return orgMapper.selectList(wrapper);
    }

    private SysOrg buildTreeNode(SysOrg org, Map<Long, List<SysOrg>> childrenByParentId, Integer type) {
        List<SysOrg> matchedChildren = childrenByParentId.getOrDefault(org.getId(), List.of()).stream()
                .map(child -> buildTreeNode(child, childrenByParentId, type))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        boolean matchedCurrent = type == null || type.equals(org.getOrgType());
        if (!matchedCurrent && matchedChildren.isEmpty()) {
            return null;
        }

        org.setChildren(matchedChildren);
        return org;
    }

    @Override
    public SysOrg getById(Long id) {
        SysOrg org = orgMapper.selectById(id);
        Require.notNull(org, PostCode.ORG_NOT_FOUND);
        return org;
    }

    @Override
    public Long create(CreateOrgCommand command) {
        validateOrg(command.getPid(), command.getOrgType(), command.getOrgCode(), null);
        SysOrg org = new SysOrg();
        org.setPid(command.getPid());
        org.setOrgName(command.getOrgName());
        org.setOrgCode(command.getOrgCode());
        org.setOrgType(command.getOrgType());
        org.setOrgSort(command.getOrgSort() == null ? 0 : command.getOrgSort());
        org.setOrgStatus(StringUtils.hasText(command.getOrgStatus()) ? command.getOrgStatus() : "1");
        orgMapper.insert(org);
        return org.getId();
    }

    @Override
    public void update(UpdateOrgCommand command) {
        SysOrg existing = getById(command.getId());
        validateOrg(command.getPid(), command.getOrgType(), command.getOrgCode(), command.getId());
        String orgStatus = StringUtils.hasText(command.getOrgStatus()) ? command.getOrgStatus() : "1";
        validateMove(existing, command.getPid(), orgStatus);

        SysOrg org = new SysOrg();
        org.setId(command.getId());
        org.setPid(command.getPid());
        org.setOrgName(command.getOrgName());
        org.setOrgCode(command.getOrgCode());
        org.setOrgType(command.getOrgType());
        org.setOrgSort(command.getOrgSort() == null ? 0 : command.getOrgSort());
        org.setOrgStatus(orgStatus);
        orgMapper.updateById(org);
    }

    @Override
    public void delete(Long id) {
        SysOrg org = getById(id);
        Require.isFalse(isRoot(org), PostCode.ORG_ROOT_DELETE_FORBIDDEN);
        Long childCount = orgMapper.selectCount(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getPid, id));
        Require.isTrue(childCount == null || childCount == 0, PostCode.ORG_HAS_CHILDREN);
        orgMapper.deleteById(id);
    }

    @Override
    public List<OrgMemberVO> members(Long orgId) {
        SysOrg org = getById(orgId);
        Long tenantId = org.getTenantId();
        List<TenantMemberOrgEntity> relations = tenantMemberOrgMapper.selectList(
                new LambdaQueryWrapper<TenantMemberOrgEntity>()
                        .eq(TenantMemberOrgEntity::getTenantId, tenantId)
                        .eq(TenantMemberOrgEntity::getOrgId, orgId)
                        .orderByDesc(TenantMemberOrgEntity::getPrimaryFlag)
                        .orderByAsc(TenantMemberOrgEntity::getCreatedAt)
                        .orderByAsc(TenantMemberOrgEntity::getId));
        if (relations == null || relations.isEmpty()) {
            return List.of();
        }
        return relations.stream().map(this::toMemberVO).toList();
    }

    @Override
    @Transactional
    public void addMember(Long orgId, AddOrgMemberCommand command) {
        SysOrg org = getById(orgId);
        TenantMember member = tenantMemberMapper.selectById(command.getMemberId());
        Require.notNull(member, PostCode.ORG_MEMBER_NOT_FOUND);
        Require.isTrue(org.getTenantId().equals(member.getTenantId()), PostCode.ORG_MEMBER_NOT_FOUND);
        if (command.getPostId() != null) {
            validatePost(org.getTenantId(), command.getPostId());
        }

        Long count = tenantMemberOrgMapper.selectCount(new LambdaQueryWrapper<TenantMemberOrgEntity>()
                .eq(TenantMemberOrgEntity::getTenantId, org.getTenantId())
                .eq(TenantMemberOrgEntity::getMemberId, command.getMemberId())
                .eq(TenantMemberOrgEntity::getOrgId, orgId));
        Require.isTrue(count == null || count == 0, PostCode.ORG_MEMBER_EXISTS);

        boolean primary = Boolean.TRUE.equals(command.getPrimaryFlag()) || member.getPrimaryOrgId() == null;
        if (primary) {
            clearMemberPrimaryOrg(org.getTenantId(), command.getMemberId());
            member.setPrimaryOrgId(orgId);
            member.setPrimaryPostId(command.getPostId());
            tenantMemberMapper.updateById(member);
        }

        TenantMemberOrgEntity relation = new TenantMemberOrgEntity();
        relation.setTenantId(org.getTenantId());
        relation.setMemberId(command.getMemberId());
        relation.setOrgId(orgId);
        relation.setPostId(command.getPostId());
        relation.setPrimaryFlag(primary ? 1 : 0);
        if (command.getLeaderFlag() != null) {
            relation.setLeaderFlag(Boolean.TRUE.equals(command.getLeaderFlag()) ? 1 : 0);
        }
        relation.setCreatedBy(MangoContextHolder.userId());
        relation.setUpdatedBy(MangoContextHolder.userId());
        tenantMemberOrgMapper.insert(relation);
    }

    @Override
    @Transactional
    public void updateMember(UpdateOrgMemberCommand command) {
        TenantMemberOrgEntity relation = tenantMemberOrgMapper.selectById(command.getRelationId());
        Require.notNull(relation, PostCode.ORG_MEMBER_RELATION_NOT_FOUND);
        if (command.getPostId() != null) {
            validatePost(relation.getTenantId(), command.getPostId());
        }
        TenantMember member = tenantMemberMapper.selectById(relation.getMemberId());
        Require.notNull(member, PostCode.ORG_MEMBER_NOT_FOUND);

        boolean primary = Boolean.TRUE.equals(command.getPrimaryFlag());
        if (primary) {
            clearMemberPrimaryOrg(relation.getTenantId(), relation.getMemberId());
            member.setPrimaryOrgId(relation.getOrgId());
            member.setPrimaryPostId(command.getPostId());
            tenantMemberMapper.updateById(member);
        } else if (isPrimaryRelation(relation)) {
            Require.isTrue(hasOtherPrimaryCandidate(relation), PostCode.ORG_MEMBER_PRIMARY_REQUIRED);
            member.setPrimaryOrgId(null);
            member.setPrimaryPostId(null);
            tenantMemberMapper.updateById(member);
        }

        relation.setPostId(command.getPostId());
        relation.setPrimaryFlag(primary ? 1 : 0);
        relation.setLeaderFlag(Boolean.TRUE.equals(command.getLeaderFlag()) ? 1 : 0);
        relation.setUpdatedBy(MangoContextHolder.userId());
        tenantMemberOrgMapper.updateById(relation);
    }

    @Override
    @Transactional
    public void removeMember(Long relationId) {
        TenantMemberOrgEntity relation = tenantMemberOrgMapper.selectById(relationId);
        Require.notNull(relation, PostCode.ORG_MEMBER_RELATION_NOT_FOUND);
        if (isPrimaryRelation(relation)) {
            Require.isTrue(hasOtherPrimaryCandidate(relation), PostCode.ORG_MEMBER_PRIMARY_REQUIRED);
        }
        tenantMemberOrgMapper.deleteById(relationId);
        TenantMember member = tenantMemberMapper.selectById(relation.getMemberId());
        if (member != null && relation.getOrgId().equals(member.getPrimaryOrgId())) {
            TenantMemberOrgEntity next = tenantMemberOrgMapper.selectOne(
                    new LambdaQueryWrapper<TenantMemberOrgEntity>()
                            .eq(TenantMemberOrgEntity::getTenantId, relation.getTenantId())
                            .eq(TenantMemberOrgEntity::getMemberId, relation.getMemberId())
                            .orderByDesc(TenantMemberOrgEntity::getPrimaryFlag)
                            .orderByAsc(TenantMemberOrgEntity::getId)
                            .last("LIMIT 1"));
            member.setPrimaryOrgId(next == null ? null : next.getOrgId());
            member.setPrimaryPostId(next == null ? null : next.getPostId());
            tenantMemberMapper.updateById(member);
            if (next != null && !Integer.valueOf(1).equals(next.getPrimaryFlag())) {
                next.setPrimaryFlag(1);
                tenantMemberOrgMapper.updateById(next);
            }
        }
    }

    @Override
    public List<Long> leaderUserIds(Long orgId) {
        SysOrg org = getById(orgId);
        List<Long> leaderPostIds = leaderPostIds(org.getTenantId());
        List<TenantMemberOrgEntity> relations = tenantMemberOrgMapper.selectList(
                new LambdaQueryWrapper<TenantMemberOrgEntity>()
                        .eq(TenantMemberOrgEntity::getTenantId, org.getTenantId())
                        .eq(TenantMemberOrgEntity::getOrgId, orgId));
        if (relations == null || relations.isEmpty()) {
            return List.of();
        }
        List<Long> memberIds = relations.stream()
                .filter(relation -> Integer.valueOf(1).equals(relation.getLeaderFlag())
                        || (relation.getPostId() != null && leaderPostIds.contains(relation.getPostId())))
                .map(TenantMemberOrgEntity::getMemberId)
                .distinct()
                .toList();
        if (memberIds.isEmpty()) {
            return List.of();
        }
        return tenantMemberMapper.selectBatchIds(memberIds).stream()
                .filter(member -> member != null && Integer.valueOf(1).equals(member.getStatus()))
                .map(TenantMember::getUserId)
                .distinct()
                .toList();
    }

    private void validateOrg(Long pid, Integer orgType, String orgCode, Long currentId) {
        Require.notNull(pid, PostCode.ORG_PARENT_REQUIRED);
        Require.notNull(orgType, PostCode.ORG_TYPE_REQUIRED);
        Require.isTrue(orgType >= 1 && orgType <= 4, PostCode.ORG_TYPE_INVALID);
        Require.notBlank(orgCode, PostCode.ORG_CODE_REQUIRED);
        Require.isFalse(currentId == null && Long.valueOf(0L).equals(pid), PostCode.ORG_ROOT_MANUAL_CREATE_FORBIDDEN);

        if (pid != 0L) {
            SysOrg parent = getById(pid);
            Require.isFalse("0".equals(parent.getOrgStatus()), PostCode.ORG_PARENT_DISABLED);
        }

        LambdaQueryWrapper<SysOrg> codeWrapper = new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getOrgCode, orgCode);
        if (currentId != null) {
            codeWrapper.ne(SysOrg::getId, currentId);
        }
        Long count = orgMapper.selectCount(codeWrapper);
        Require.isTrue(count == null || count == 0, PostCode.ORG_CODE_EXISTS);
    }

    private void validateMove(SysOrg existing, Long targetPid, String orgStatus) {
        if (isRoot(existing)) {
            Require.isTrue(targetPid == 0L, PostCode.ORG_ROOT_MOVE_FORBIDDEN);
            Require.isFalse("0".equals(orgStatus), PostCode.ORG_ROOT_DISABLE_FORBIDDEN);
            return;
        }
        Require.isFalse(existing.getId().equals(targetPid), PostCode.ORG_PARENT_SELF_FORBIDDEN);
        Long cursor = targetPid;
        while (cursor != null && cursor != 0L) {
            Require.isFalse(existing.getId().equals(cursor), PostCode.ORG_PARENT_DESCENDANT_FORBIDDEN);
            SysOrg parent = orgMapper.selectById(cursor);
            if (parent == null) {
                return;
            }
            cursor = parent.getPid();
        }
    }

    private boolean isRoot(SysOrg org) {
        return org != null && (org.getPid() == null || org.getPid() == 0L);
    }

    private void validatePost(Long tenantId, Long postId) {
        PostEntity post = postMapper.selectById(postId);
        Require.notNull(post, PostCode.POST_NOT_FOUND);
        Require.isTrue(tenantId.equals(post.getTenantId()), PostCode.POST_NOT_FOUND);
        Require.isFalse("0".equals(post.getPostStatus()), PostCode.POST_NOT_FOUND);
    }

    private OrgMemberVO toMemberVO(TenantMemberOrgEntity relation) {
        OrgMemberVO vo = new OrgMemberVO();
        vo.setRelationId(relation.getId());
        vo.setMemberId(relation.getMemberId());
        vo.setOrgId(relation.getOrgId());
        vo.setPostId(relation.getPostId());
        vo.setPrimaryFlag(Integer.valueOf(1).equals(relation.getPrimaryFlag()));

        TenantMember member = tenantMemberMapper.selectById(relation.getMemberId());
        if (member != null) {
            vo.setUserId(member.getUserId());
            vo.setMemberName(member.getDisplayName());
            vo.setMemberType(member.getMemberType());
            vo.setStatus(member.getStatus());
            IdentityUser user = identityUserMapper.selectById(member.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
            }
        }
        PostEntity post = relation.getPostId() == null ? null : postMapper.selectById(relation.getPostId());
        if (post != null) {
            vo.setPostName(post.getPostName());
            vo.setPostCode(post.getPostCode());
            vo.setLeaderFlag(isLeaderRelation(relation, post));
        } else {
            vo.setLeaderFlag(isLeaderRelation(relation, null));
        }
        return vo;
    }

    private void clearMemberPrimaryOrg(Long tenantId, Long memberId) {
        List<TenantMemberOrgEntity> relations = tenantMemberOrgMapper.selectList(
                new LambdaQueryWrapper<TenantMemberOrgEntity>()
                        .eq(TenantMemberOrgEntity::getTenantId, tenantId)
                        .eq(TenantMemberOrgEntity::getMemberId, memberId)
                        .eq(TenantMemberOrgEntity::getPrimaryFlag, 1));
        relations.forEach(relation -> {
            relation.setPrimaryFlag(0);
            tenantMemberOrgMapper.updateById(relation);
        });
    }

    private boolean isPrimaryRelation(TenantMemberOrgEntity relation) {
        return Integer.valueOf(1).equals(relation.getPrimaryFlag());
    }

    private boolean hasOtherPrimaryCandidate(TenantMemberOrgEntity relation) {
        Long count = tenantMemberOrgMapper.selectCount(new LambdaQueryWrapper<TenantMemberOrgEntity>()
                .eq(TenantMemberOrgEntity::getTenantId, relation.getTenantId())
                .eq(TenantMemberOrgEntity::getMemberId, relation.getMemberId())
                .ne(TenantMemberOrgEntity::getId, relation.getId()));
        return count != null && count > 0;
    }

    private List<Long> leaderPostIds(Long tenantId) {
        List<PostEntity> posts = postMapper.selectList(new LambdaQueryWrapper<PostEntity>()
                .eq(PostEntity::getTenantId, tenantId)
                .eq(PostEntity::getPostStatus, "1"));
        if (posts == null || posts.isEmpty()) {
            return Collections.emptyList();
        }
        return posts.stream()
                .filter(this::isLeaderPost)
                .map(PostEntity::getId)
                .distinct()
                .toList();
    }

    private boolean isLeaderPost(PostEntity post) {
        if (post == null || !StringUtils.hasText(post.getPostCode())) {
            return false;
        }
        String code = post.getPostCode().trim().toUpperCase();
        return code.equals(DEPT_MANAGER_POST_CODE)
                || code.equals(ORG_MANAGER_POST_CODE)
                || code.equals(TEAM_LEADER_POST_CODE)
                || code.endsWith("_" + DEPT_MANAGER_POST_CODE)
                || code.endsWith("_" + ORG_MANAGER_POST_CODE)
                || code.endsWith("_" + TEAM_LEADER_POST_CODE);
    }

    private boolean isLeaderRelation(TenantMemberOrgEntity relation, PostEntity post) {
        if (relation != null && Integer.valueOf(1).equals(relation.getLeaderFlag())) {
            return true;
        }
        return isLeaderPost(post);
    }
}
