package io.mango.identity.starter;

import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.vo.AuthUserInfo;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.service.IIdentityUserService;

/**
 * 基于本地 identity 服务的认证用户事实 Provider。
 */
public class IdentityAuthUserProvider implements AuthUserProvider {

    private final IIdentityUserService identityUserService;

    public IdentityAuthUserProvider(IIdentityUserService identityUserService) {
        this.identityUserService = identityUserService;
    }

    @Override
    public AuthUserInfo getByUsernameForAuth(String username) {
        return toAuthUserInfo(identityUserService.getByUsername(username));
    }

    @Override
    public AuthUserInfo getByUsernameForAuth(String username, String realm) {
        return toAuthUserInfo(identityUserService.getByUsername(username, realm));
    }

    @Override
    public AuthUserInfo getByIdForAuth(Long userId) {
        return toAuthUserInfo(identityUserService.getById(userId));
    }

    private AuthUserInfo toAuthUserInfo(IdentityUser entity) {
        if (entity == null) {
            return null;
        }
        AuthUserInfo authUser = new AuthUserInfo();
        authUser.setUserId(entity.getUserId());
        authUser.setUsername(entity.getUsername());
        authUser.setPassword(entity.getPassword());
        authUser.setNickname(entity.getNickname());
        authUser.setRealm(entity.getRealm());
        authUser.setActorType(entity.getActorType());
        authUser.setPartyType(entity.getPartyType());
        authUser.setPartyId(entity.getPartyId());
        authUser.setStatus(entity.getStatus());
        authUser.setPasswordResetRequired(entity.getPasswordResetRequired());
        authUser.setPasswordUpdatedAt(entity.getPasswordUpdatedAt());
        authUser.setFailedLoginCount(entity.getFailedLoginCount());
        authUser.setLastFailedLoginAt(entity.getLastFailedLoginAt());
        authUser.setLockedUntil(entity.getLockedUntil());
        authUser.setLockedReason(entity.getLockedReason());
        return authUser;
    }
}
