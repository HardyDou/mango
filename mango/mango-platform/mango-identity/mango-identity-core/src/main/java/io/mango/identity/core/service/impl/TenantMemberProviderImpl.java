package io.mango.identity.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.vo.TenantMemberInfo;
import io.mango.identity.core.entity.TenantMember;
import io.mango.identity.core.mapper.TenantMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基于本地租户成员表的成员事实 Provider。
 */
@Service
@RequiredArgsConstructor
public class TenantMemberProviderImpl implements TenantMemberProvider {

    private static final int STATUS_ENABLED = 1;

    private final TenantMemberMapper tenantMemberMapper;

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
}
