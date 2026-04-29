package io.mango.identity.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.identity.api.vo.IdentityUserInfo;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.service.IIdentityUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 身份用户服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityUserServiceImpl implements IIdentityUserService {

    private static final String DEFAULT_REALM = "INTERNAL";

    private final IdentityUserMapper identityUserMapper;

    @Override
    public IdentityUserInfo getUserInfo(String username) {
        IdentityUser user = getByUsername(username);
        if (user == null) {
            log.warn("Identity user not found: {}", username);
            return null;
        }
        return buildIdentityUserInfo(user);
    }

    @Override
    public IdentityUserInfo getUserInfoById(Long userId) {
        IdentityUser user = getById(userId);
        if (user == null) {
            log.warn("Identity user not found by id: {}", userId);
            return null;
        }
        return buildIdentityUserInfo(user);
    }

    @Override
    public IdentityUser getByUsername(String username) {
        return getByUsername(username, DEFAULT_REALM);
    }

    @Override
    public IdentityUser getByUsername(String username, String realm) {
        LambdaQueryWrapper<IdentityUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IdentityUser::getUsername, username)
                .eq(IdentityUser::getRealm, normalizeRealm(realm));
        return identityUserMapper.selectOne(wrapper);
    }

    @Override
    public IdentityUser getById(Long userId) {
        return identityUserMapper.selectById(userId);
    }

    /**
     * 从账号资料构造身份资料 VO。
     */
    private IdentityUserInfo buildIdentityUserInfo(IdentityUser user) {
        IdentityUserInfo userInfo = new IdentityUserInfo();
        userInfo.setUserId(user.getUserId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setRealm(user.getRealm());
        userInfo.setActorType(user.getActorType());
        userInfo.setPartyType(user.getPartyType());
        userInfo.setPartyId(user.getPartyId());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setStatus(user.getStatus());

        return userInfo;
    }

    private String normalizeRealm(String realm) {
        return realm == null || realm.isBlank() ? DEFAULT_REALM : realm.trim();
    }
}
