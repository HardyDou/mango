package io.mango.identity.starter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.AuthorizationSubjectReferenceProvider;
import io.mango.identity.core.entity.IdentityUserEntity;
import io.mango.identity.core.entity.TenantMemberEntity;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 基于身份域数据解析授权资源中的机构成员引用。 */
@Component
@RequiredArgsConstructor
public class IdentityAuthorizationSubjectReferenceProvider implements AuthorizationSubjectReferenceProvider {

    private final TenantMemberMapper memberMapper;
    private final IdentityUserMapper userMapper;

    @Override
    public Long resolveMemberId(Long tenantId, String memberNo, String username) {
        TenantMemberEntity member = memberByNo(tenantId, memberNo);
        if (member != null) {
            return member.getMemberId();
        }
        if (!StringUtils.hasText(username)) {
            return null;
        }
        IdentityUserEntity user = userMapper.selectOne(new LambdaQueryWrapper<IdentityUserEntity>()
                .eq(IdentityUserEntity::getUsername, username.trim())
                .last("LIMIT 1"));
        if (user == null) {
            return null;
        }
        member = memberMapper.selectOne(new LambdaQueryWrapper<TenantMemberEntity>()
                .eq(TenantMemberEntity::getTenantId, tenantId)
                .eq(TenantMemberEntity::getUserId, user.getUserId())
                .isNull(TenantMemberEntity::getLeftAt)
                .last("LIMIT 1"));
        return member == null ? null : member.getMemberId();
    }

    private TenantMemberEntity memberByNo(Long tenantId, String memberNo) {
        if (!StringUtils.hasText(memberNo)) {
            return null;
        }
        return memberMapper.selectOne(new LambdaQueryWrapper<TenantMemberEntity>()
                .eq(TenantMemberEntity::getTenantId, tenantId)
                .eq(TenantMemberEntity::getMemberNo, memberNo.trim())
                .isNull(TenantMemberEntity::getLeftAt)
                .last("LIMIT 1"));
    }
}
