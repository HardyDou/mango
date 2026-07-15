package io.mango.identity.starter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.AuthorizationSubjectReferenceProvider;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.entity.TenantMember;
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
        TenantMember member = memberByNo(tenantId, memberNo);
        if (member != null) {
            return member.getMemberId();
        }
        if (!StringUtils.hasText(username)) {
            return null;
        }
        IdentityUser user = userMapper.selectOne(new LambdaQueryWrapper<IdentityUser>()
                .eq(IdentityUser::getUsername, username.trim())
                .last("LIMIT 1"));
        if (user == null) {
            return null;
        }
        member = memberMapper.selectOne(new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getTenantId, tenantId)
                .eq(TenantMember::getUserId, user.getUserId())
                .isNull(TenantMember::getLeftAt)
                .last("LIMIT 1"));
        return member == null ? null : member.getMemberId();
    }

    private TenantMember memberByNo(Long tenantId, String memberNo) {
        if (!StringUtils.hasText(memberNo)) {
            return null;
        }
        return memberMapper.selectOne(new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getTenantId, tenantId)
                .eq(TenantMember::getMemberNo, memberNo.trim())
                .isNull(TenantMember::getLeftAt)
                .last("LIMIT 1"));
    }
}
