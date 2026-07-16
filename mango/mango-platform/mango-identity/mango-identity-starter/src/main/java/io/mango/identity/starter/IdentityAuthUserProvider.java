package io.mango.identity.starter;

import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.vo.AuthUserVO;
import io.mango.identity.core.entity.IdentityUserEntity;
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
    public AuthUserVO getByUsernameForAuth(String username) {
        return toAuthUserVO(identityUserService.getByUsername(username));
    }

    @Override
    public AuthUserVO getByUsernameForAuth(String username, String realm) {
        return toAuthUserVO(identityUserService.getByUsername(username, realm));
    }

    @Override
    public AuthUserVO getByIdForAuth(Long userId) {
        return toAuthUserVO(identityUserService.getById(userId));
    }

    private AuthUserVO toAuthUserVO(IdentityUserEntity entity) {
        if (entity == null) {
            return null;
        }
        AuthUserVO authUser = new AuthUserVO();
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
