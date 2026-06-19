package io.mango.identity.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.command.AddTenantMemberOrgCommand;
import io.mango.identity.api.command.UpdateTenantMemberOrgCommand;
import io.mango.identity.api.vo.TenantMemberOrgRelationInfo;
import io.mango.identity.api.vo.TenantMemberInfo;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.entity.TenantMember;
import io.mango.identity.core.entity.TenantMemberOrgEntity;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.identity.core.mapper.TenantMemberOrgMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 基于本地租户成员表的成员事实 Provider。
 */
@Service
@RequiredArgsConstructor
public class TenantMemberProviderImpl implements TenantMemberProvider {

    private static final int STATUS_ENABLED = 1;

    private final TenantMemberMapper tenantMemberMapper;
    private final TenantMemberOrgMapper tenantMemberOrgMapper;
    private final IdentityUserMapper identityUserMapper;

    @Override
    public TenantMemberInfo getEnabledMember(Long userId, Long tenantId) {
        if (userId == null || tenantId == null) {
            return null;
        }
        TenantMember member = tenantMemberMapper.selectOne(new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getUserId, userId)
                .eq(TenantMember::getTenantId, tenantId)
                .eq(TenantMember::getStatus, STATUS_ENABLED)
                .last("LIMIT 1"));
        return toInfo(member);
    }

    @Override
    public List<TenantMemberInfo> listEnabledMembers(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return tenantMemberMapper.selectList(new LambdaQueryWrapper<TenantMember>()
                        .eq(TenantMember::getUserId, userId)
                        .eq(TenantMember::getStatus, STATUS_ENABLED)
                        .orderByAsc(TenantMember::getTenantId))
                .stream()
                .map(this::toInfo)
                .toList();
    }

    @Override
    public TenantMemberInfo getMember(Long memberId) {
        if (memberId == null) {
            return null;
        }
        return toInfo(tenantMemberMapper.selectById(memberId));
    }

    @Override
    public List<TenantMemberOrgRelationInfo> listOrgRelations(Long tenantId, Long orgId) {
        if (tenantId == null || orgId == null) {
            return List.of();
        }
        return tenantMemberOrgMapper.selectList(new LambdaQueryWrapper<TenantMemberOrgEntity>()
                        .eq(TenantMemberOrgEntity::getTenantId, tenantId)
                        .eq(TenantMemberOrgEntity::getOrgId, orgId)
                        .orderByDesc(TenantMemberOrgEntity::getPrimaryFlag)
                        .orderByAsc(TenantMemberOrgEntity::getCreatedAt)
                        .orderByAsc(TenantMemberOrgEntity::getId))
                .stream()
                .map(this::toRelationInfo)
                .toList();
    }

    @Override
    public TenantMemberOrgRelationInfo getOrgRelation(Long relationId) {
        if (relationId == null) {
            return null;
        }
        return toRelationInfo(tenantMemberOrgMapper.selectById(relationId));
    }

    @Override
    public boolean existsOrgRelation(Long tenantId, Long memberId, Long orgId) {
        if (tenantId == null || memberId == null || orgId == null) {
            return false;
        }
        Long count = tenantMemberOrgMapper.selectCount(new LambdaQueryWrapper<TenantMemberOrgEntity>()
                .eq(TenantMemberOrgEntity::getTenantId, tenantId)
                .eq(TenantMemberOrgEntity::getMemberId, memberId)
                .eq(TenantMemberOrgEntity::getOrgId, orgId));
        return count != null && count > 0;
    }

    @Override
    @Transactional
    public void addOrgRelation(AddTenantMemberOrgCommand command) {
        TenantMember member = tenantMemberMapper.selectById(command.getMemberId());
        boolean primary = Boolean.TRUE.equals(command.getPrimaryFlag())
                || member != null && member.getPrimaryOrgId() == null;
        if (primary) {
            clearPrimaryOrg(command.getTenantId(), command.getMemberId());
            if (member != null) {
                member.setPrimaryOrgId(command.getOrgId());
                member.setPrimaryPostId(command.getPostId());
                tenantMemberMapper.updateById(member);
            }
        }
        TenantMemberOrgEntity relation = new TenantMemberOrgEntity();
        relation.setTenantId(command.getTenantId());
        relation.setMemberId(command.getMemberId());
        relation.setOrgId(command.getOrgId());
        relation.setPostId(command.getPostId());
        relation.setPrimaryFlag(primary ? 1 : 0);
        if (command.getLeaderFlag() != null) {
            relation.setLeaderFlag(Boolean.TRUE.equals(command.getLeaderFlag()) ? 1 : 0);
        }
        relation.setCreatedBy(command.getOperatorUserId());
        relation.setUpdatedBy(command.getOperatorUserId());
        tenantMemberOrgMapper.insert(relation);
    }

    @Override
    @Transactional
    public void updateOrgRelation(UpdateTenantMemberOrgCommand command) {
        TenantMemberOrgEntity relation = tenantMemberOrgMapper.selectById(command.getRelationId());
        if (relation == null) {
            return;
        }
        TenantMember member = tenantMemberMapper.selectById(relation.getMemberId());
        boolean primary = Boolean.TRUE.equals(command.getPrimaryFlag());
        if (primary) {
            clearPrimaryOrg(relation.getTenantId(), relation.getMemberId());
            if (member != null) {
                member.setPrimaryOrgId(relation.getOrgId());
                member.setPrimaryPostId(command.getPostId());
                tenantMemberMapper.updateById(member);
            }
        } else if (Integer.valueOf(1).equals(relation.getPrimaryFlag()) && member != null) {
            member.setPrimaryOrgId(null);
            member.setPrimaryPostId(null);
            tenantMemberMapper.updateById(member);
        }
        relation.setPostId(command.getPostId());
        relation.setPrimaryFlag(primary ? 1 : 0);
        relation.setLeaderFlag(Boolean.TRUE.equals(command.getLeaderFlag()) ? 1 : 0);
        relation.setUpdatedBy(command.getOperatorUserId());
        tenantMemberOrgMapper.updateById(relation);
    }

    @Override
    @Transactional
    public void removeOrgRelation(Long relationId) {
        TenantMemberOrgEntity relation = tenantMemberOrgMapper.selectById(relationId);
        if (relation == null) {
            return;
        }
        tenantMemberOrgMapper.deleteById(relationId);
        TenantMember member = tenantMemberMapper.selectById(relation.getMemberId());
        if (member == null || !relation.getOrgId().equals(member.getPrimaryOrgId())) {
            return;
        }
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

    @Override
    public long countOtherOrgRelations(Long tenantId, Long memberId, Long excludedRelationId) {
        if (tenantId == null || memberId == null) {
            return 0;
        }
        Long count = tenantMemberOrgMapper.selectCount(new LambdaQueryWrapper<TenantMemberOrgEntity>()
                .eq(TenantMemberOrgEntity::getTenantId, tenantId)
                .eq(TenantMemberOrgEntity::getMemberId, memberId)
                .ne(excludedRelationId != null, TenantMemberOrgEntity::getId, excludedRelationId));
        return count == null ? 0 : count;
    }

    @Override
    public List<TenantMemberInfo> listMembers(Collection<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }
        return tenantMemberMapper.selectBatchIds(memberIds)
                .stream()
                .map(this::toInfo)
                .toList();
    }

    private TenantMemberInfo toInfo(TenantMember member) {
        if (member == null) {
            return null;
        }
        TenantMemberInfo info = new TenantMemberInfo();
        info.setMemberId(member.getMemberId());
        info.setTenantId(member.getTenantId());
        info.setUserId(member.getUserId());
        info.setMemberNo(member.getMemberNo());
        info.setDisplayName(member.getDisplayName());
        info.setMemberType(member.getMemberType());
        info.setStatus(member.getStatus());
        info.setPrimaryOrgId(member.getPrimaryOrgId());
        info.setPrimaryPostId(member.getPrimaryPostId());
        return info;
    }

    private void clearPrimaryOrg(Long tenantId, Long memberId) {
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

    private TenantMemberOrgRelationInfo toRelationInfo(TenantMemberOrgEntity relation) {
        if (relation == null) {
            return null;
        }
        TenantMemberOrgRelationInfo info = new TenantMemberOrgRelationInfo();
        info.setRelationId(relation.getId());
        info.setTenantId(relation.getTenantId());
        info.setMemberId(relation.getMemberId());
        info.setOrgId(relation.getOrgId());
        info.setPostId(relation.getPostId());
        info.setPrimaryFlag(Integer.valueOf(1).equals(relation.getPrimaryFlag()));
        info.setLeaderFlag(Integer.valueOf(1).equals(relation.getLeaderFlag()));
        TenantMember member = tenantMemberMapper.selectById(relation.getMemberId());
        if (member != null) {
            info.setUserId(member.getUserId());
            info.setDisplayName(member.getDisplayName());
            info.setMemberType(member.getMemberType());
            info.setStatus(member.getStatus());
            IdentityUser user = identityUserMapper.selectById(member.getUserId());
            if (user != null) {
                info.setUsername(user.getUsername());
                info.setNickname(user.getNickname());
            }
        }
        return info;
    }
}
