package io.mango.identity.core.service.impl;

import io.mango.common.result.Require;
import io.mango.identity.api.AuthIdentitySecurityProvider;
import io.mango.identity.api.AuthUserProvider;
import io.mango.identity.api.command.ChangeRequiredPasswordCommand;
import io.mango.identity.api.enums.IdentityCode;
import io.mango.identity.api.query.AuthUsernameQuery;
import io.mango.identity.api.vo.AuthUserVO;
import io.mango.identity.core.service.IAuthIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthIdentityService implements IAuthIdentityService {

    private final AuthUserProvider authUserProvider;
    private final AuthIdentitySecurityProvider authIdentitySecurityProvider;

    @Override
    public AuthUserVO getByUsernameForAuth(AuthUsernameQuery query) {
        return authUserProvider.getByUsernameForAuth(query.getUsername(), query.getRealm());
    }

    @Override
    public AuthUserVO getByIdForAuth(Long userId) {
        return authUserProvider.getByIdForAuth(userId);
    }

    @Override
    public boolean recordLoginFailure(Long userId) {
        authIdentitySecurityProvider.recordLoginFailure(userId);
        return true;
    }

    @Override
    public boolean recordLoginSuccess(Long userId) {
        authIdentitySecurityProvider.recordLoginSuccess(userId);
        return true;
    }

    @Override
    public boolean changeRequiredPassword(ChangeRequiredPasswordCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "修改密码命令不能为空");
        authIdentitySecurityProvider.changeRequiredPassword(command);
        return true;
    }
}
