package io.mango.identity.core.service;

import io.mango.identity.api.command.ChangeRequiredPasswordCommand;
import io.mango.identity.api.query.AuthUsernameQuery;
import io.mango.identity.api.vo.AuthUserVO;

public interface IAuthIdentityService {

    AuthUserVO getByUsernameForAuth(AuthUsernameQuery query);

    AuthUserVO getByIdForAuth(Long userId);

    boolean recordLoginFailure(Long userId);

    boolean recordLoginSuccess(Long userId);

    boolean changeRequiredPassword(ChangeRequiredPasswordCommand command);
}
