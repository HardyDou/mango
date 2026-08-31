package io.mango.identity.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.command.AddTenantMemberOrgCommand;
import io.mango.identity.api.command.CreateIdentityUserCommand;
import io.mango.identity.api.command.CreateTenantMemberInOrgCommand;
import io.mango.identity.api.command.UpdateTenantMemberOrgCommand;
import io.mango.identity.api.enums.IdentityCode;
import io.mango.identity.api.vo.TenantMemberOrgRelationVO;
import io.mango.identity.api.vo.TenantMemberVO;
import io.mango.identity.core.entity.IdentityUserEntity;
import io.mango.identity.core.entity.TenantMemberEntity;
import io.mango.identity.core.entity.TenantMemberOrgEntity;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.identity.core.mapper.TenantMemberOrgMapper;
import io.mango.identity.core.service.IIdentityUserService;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 基于本地租户成员表的成员事实 Provider。
 */
@Component
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring singleton collaborators are intentionally injected and retained"))
public class LocalTenantMemberProvider implements TenantMemberProvider {

    private static final int STATUS_ENABLED = 1;

    private final TenantMemberMapper tenantMemberMapper;
    private final TenantMemberOrgMapper tenantMemberOrgMapper;
    private final IdentityUserMapper identityUserMapper;
    private final IIdentityUserService identityUserService;

    @Override
    @Transactional
    public Long createMemberInOrg(CreateTenantMemberInOrgCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "组织成员账号创建命令不能为空");
        Long currentTenantId = currentTenantId();
        Require.isTrue(command.getTenantId().equals(currentTenantId), IdentityCode.VALIDATION_ERROR,
                "只能在当前机构内创建成员账号");

        CreateIdentityUserCommand userCommand = new CreateIdentityUserCommand();
        userCommand.setUsername(command.getUsername());
        userCommand.setPassword(command.getPassword());
        userCommand.setNickname(command.getNickname());
        userCommand.setEmail(command.getEmail());
        userCommand.setPhone(command.getPhone());
        userCommand.setStatus(command.getStatus());
        userCommand.setRemark(command.getRemark());
        Long userId = identityUserService.create(userCommand);

        TenantMemberEntity member = tenantMemberMapper.selectOne(new LambdaQueryWrapper<TenantMemberEntity>()
                .eq(TenantMemberEntity::getTenantId, currentTenantId)
                .eq(TenantMemberEntity::getUserId, userId)
                .last("LIMIT 1"));
        Require.notNull(member, IdentityCode.CONFLICT, "租户成员创建失败");

        AddTenantMemberOrgCommand relationCommand = new AddTenantMemberOrgCommand();
        relationCommand.setTenantId(currentTenantId);
        relationCommand.setMemberId(member.getMemberId());
        relationCommand.setOrgId(command.getOrgId());
        relationCommand.setPostId(command.getPostId());
        relationCommand.setPrimaryFlag(command.getPrimaryFlag());
        relationCommand.setLeaderFlag(command.getLeaderFlag());
        relationCommand.setOperatorUserId(command.getOperatorUserId());
        addOrgRelation(relationCommand);
        return userId;
    }

    @Override
    public TenantMemberVO getEnabledMember(Long userId, Long tenantId) {
        if (userId == null || tenantId == null) {
            return null;
        }
        TenantMemberEntity member = tenantMemberMapper.selectOne(new LambdaQueryWrapper<TenantMemberEntity>()
                .eq(TenantMemberEntity::getUserId, userId)
                .eq(TenantMemberEntity::getTenantId, tenantId)
                .eq(TenantMemberEntity::getStatus, STATUS_ENABLED)
                .last("LIMIT 1"));
        return toInfo(member);
    }

    @Override
    public List<TenantMemberVO> listEnabledMembers(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return tenantMemberMapper.selectList(new LambdaQueryWrapper<TenantMemberEntity>()
                        .eq(TenantMemberEntity::getUserId, userId)
                        .eq(TenantMemberEntity::getStatus, STATUS_ENABLED)
                        .orderByAsc(TenantMemberEntity::getTenantId))
                .stream()
                .map(this::toInfo)
                .toList();
    }

    @Override
    public TenantMemberVO getMember(Long memberId) {
        if (memberId == null) {
            return null;
        }
        return toInfo(tenantMemberMapper.selectById(memberId));
    }

    @Override
    public List<TenantMemberOrgRelationVO> listOrgRelations(Long tenantId, Long orgId) {
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
    public TenantMemberOrgRelationVO getOrgRelation(Long relationId) {
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
        TenantMemberEntity member = tenantMemberMapper.selectById(command.getMemberId());
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
        relation.setTenantId(String.valueOf(command.getTenantId()));
        relation.setMemberId(command.getMemberId());
        relation.setOrgId(command.getOrgId());
        relation.setPostId(command.getPostId());
        relation.setPrimaryFlag(booleanFlag(primary));
        if (command.getLeaderFlag() != null) {
            relation.setLeaderFlag(booleanFlag(Boolean.TRUE.equals(command.getLeaderFlag())));
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
        TenantMemberEntity member = tenantMemberMapper.selectById(relation.getMemberId());
        boolean primary = Boolean.TRUE.equals(command.getPrimaryFlag());
        if (primary) {
            clearPrimaryOrg(Long.valueOf(relation.getTenantId()), relation.getMemberId());
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
        relation.setPrimaryFlag(booleanFlag(primary));
        relation.setLeaderFlag(booleanFlag(Boolean.TRUE.equals(command.getLeaderFlag())));
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
        TenantMemberEntity member = tenantMemberMapper.selectById(relation.getMemberId());
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
        if (next == null) {
            member.setPrimaryOrgId(null);
            member.setPrimaryPostId(null);
        } else {
            member.setPrimaryOrgId(next.getOrgId());
            member.setPrimaryPostId(next.getPostId());
        }
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
        if (count == null) {
            return 0;
        }
        return count;
    }

    @Override
    public List<TenantMemberVO> listMembers(Collection<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }
        return tenantMemberMapper.selectBatchIds(memberIds)
                .stream()
                .map(this::toInfo)
                .toList();
    }

    private int booleanFlag(boolean value) {
        if (value) {
            return 1;
        }
        return 0;
    }

    private Long currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, IdentityCode.VALIDATION_ERROR, "当前机构上下文无效");
        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException exception) {
            return Require.fail(IdentityCode.VALIDATION_ERROR, "当前机构上下文无效");
        }
    }

    private TenantMemberVO toInfo(TenantMemberEntity member) {
        if (member == null) {
            return null;
        }
        TenantMemberVO info = new TenantMemberVO();
        info.setMemberId(member.getMemberId());
        info.setTenantId(Long.valueOf(member.getTenantId()));
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

    private TenantMemberOrgRelationVO toRelationInfo(TenantMemberOrgEntity relation) {
        if (relation == null) {
            return null;
        }
        TenantMemberOrgRelationVO info = new TenantMemberOrgRelationVO();
        info.setRelationId(relation.getId());
        info.setTenantId(Long.valueOf(relation.getTenantId()));
        info.setMemberId(relation.getMemberId());
        info.setOrgId(relation.getOrgId());
        info.setPostId(relation.getPostId());
        info.setPrimaryFlag(Integer.valueOf(1).equals(relation.getPrimaryFlag()));
        info.setLeaderFlag(Integer.valueOf(1).equals(relation.getLeaderFlag()));
        TenantMemberEntity member = tenantMemberMapper.selectById(relation.getMemberId());
        if (member != null) {
            info.setUserId(member.getUserId());
            info.setDisplayName(member.getDisplayName());
            info.setMemberType(member.getMemberType());
            info.setStatus(member.getStatus());
            IdentityUserEntity user = identityUserMapper.selectById(member.getUserId());
            if (user != null) {
                info.setUsername(user.getUsername());
                info.setNickname(user.getNickname());
            }
        }
        return info;
    }
}
