package io.mango.identity.core.service;

import io.mango.identity.api.command.ChangeRequiredPasswordCommand;
import io.mango.identity.api.vo.AuthUserVO;
import io.mango.identity.core.entity.IdentityUserEntity;

public interface IIdentityUserSecurityService {

    void assertLoginAllowed(AuthUserVO user);
    void recordLoginFailure(Long userId);
    void recordLoginSuccess(Long userId);
    void changeRequiredPassword(ChangeRequiredPasswordCommand command);
    boolean unlock(Long userId);
    boolean requirePasswordReset(Long userId);
    boolean isLocked(IdentityUserEntity user);
}
