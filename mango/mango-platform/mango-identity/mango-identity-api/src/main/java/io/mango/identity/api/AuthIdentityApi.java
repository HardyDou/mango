package io.mango.identity.api;

import io.mango.common.result.R;
import io.mango.identity.api.command.ChangeRequiredPasswordCommand;
import io.mango.identity.api.query.AuthUsernameQuery;
import io.mango.identity.api.vo.AuthUserVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 认证用户事实内部契约。
 */
public interface AuthIdentityApi {

    /** 按登录域和用户名查询认证用户事实。 */
    R<AuthUserVO> getByUsernameForAuth(@Valid AuthUsernameQuery query);

    /** 按用户 ID 查询认证用户事实。 */
    R<AuthUserVO> getByIdForAuth(@NotNull Long userId);

    /** 记录密码校验失败。 */
    R<Boolean> recordLoginFailure(@NotNull Long userId);

    /** 记录登录成功。 */
    R<Boolean> recordLoginSuccess(@NotNull Long userId);

    /** 使用强制改密凭据修改密码。 */
    R<Boolean> changeRequiredPassword(@Valid ChangeRequiredPasswordCommand command);
}
