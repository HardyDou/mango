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
 * Identity user service implementation
 *
 * @author Mango
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityUserServiceImpl implements IIdentityUserService {

    private final IdentityUserMapper identityUserMapper;

    @Override
    public IdentityUserInfo getUserInfo(String username) {
        IdentityUser user = getByUsername(username);
        if (user == null) {
            log.warn("User not found: {}", username);
            return null;
        }
        return buildIdentityUserInfo(user);
    }

    @Override
    public IdentityUserInfo getUserInfoById(Long userId) {
        IdentityUser user = getById(userId);
        if (user == null) {
            log.warn("User not found by id: {}", userId);
            return null;
        }
        return buildIdentityUserInfo(user);
    }

    @Override
    public IdentityUser getByUsername(String username) {
        LambdaQueryWrapper<IdentityUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IdentityUser::getUsername, username);
        return identityUserMapper.selectOne(wrapper);
    }

    @Override
    public IdentityUser getById(Long userId) {
        return identityUserMapper.selectById(userId);
    }

    /**
     * Build IdentityUserInfo from account profile.
     */
    private IdentityUserInfo buildIdentityUserInfo(IdentityUser user) {
        IdentityUserInfo userInfo = new IdentityUserInfo();
        userInfo.setUserId(user.getUserId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setStatus(user.getStatus());

        return userInfo;
    }
}
