package io.mango.identity.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.identity.api.command.BatchDeleteIdentityUserCommand;
import io.mango.identity.api.command.BindExternalIdentityCommand;
import io.mango.identity.api.command.CreateIdentityUserCommand;
import io.mango.identity.api.command.RequireIdentityUserPasswordResetCommand;
import io.mango.identity.api.command.ResetIdentityUserPasswordCommand;
import io.mango.identity.api.command.UnbindExternalIdentityCommand;
import io.mango.identity.api.command.UpdateIdentityUserCommand;
import io.mango.identity.api.command.UpdateIdentityUserStatusCommand;
import io.mango.identity.api.command.UnlockIdentityUserCommand;
import io.mango.identity.api.command.SendContactCaptchaCommand;
import io.mango.identity.api.command.UpdateCurrentUserContactCommand;
import io.mango.identity.api.command.UpdateCurrentUserProfileCommand;
import io.mango.identity.api.command.UnbindCurrentExternalIdentityCommand;
import io.mango.identity.api.query.ExternalIdentityQuery;
import io.mango.identity.api.query.IdentityUserPageQuery;
import io.mango.identity.api.query.IdentityAccountAvailabilityQuery;
import io.mango.identity.api.request.IdentityUserBatchRequest;
import io.mango.identity.api.query.IdentityUserTargetQuery;
import io.mango.identity.api.vo.ExternalIdentityBindingVO;
import io.mango.identity.api.vo.IdentityUserInfoVO;
import io.mango.identity.api.vo.IdentityUserVO;
import io.mango.identity.api.vo.IdentityAccountAvailabilityVO;
import io.mango.identity.api.vo.ContactCaptchaTicketVO;
import io.mango.identity.api.vo.CurrentUserProfileVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 身份用户资料 HTTP 契约。
 */
public interface IdentityUserApi {

    R<CurrentUserProfileVO> currentProfile();

    R<CurrentUserProfileVO> updateCurrentProfile(@Valid UpdateCurrentUserProfileCommand command);

    R<ContactCaptchaTicketVO> sendCurrentContactCaptcha(@Valid SendContactCaptchaCommand command);

    R<CurrentUserProfileVO> updateCurrentContact(@Valid UpdateCurrentUserContactCommand command);

    /**
     * 分页查询当前租户成员。
     */
    R<PageResult<IdentityUserVO>> page(@Valid IdentityUserPageQuery query);

    /**
     * Query current tenant member detail by user ID.
     */
    R<IdentityUserVO> detail(@NotNull Long userId);

    /**
     * 创建当前租户成员。
     */
    R<Long> create(@Valid CreateIdentityUserCommand command);

    /** Check whether a login account can be created or restored in the current tenant. */
    R<IdentityAccountAvailabilityVO> accountAvailability(@Valid IdentityAccountAvailabilityQuery query);

    /**
     * 更新当前租户成员。
     */
    R<Boolean> update(@Valid UpdateIdentityUserCommand command);

    /**
     * 移除当前租户成员。
     */
    R<Boolean> delete(@NotNull Long userId);

    /**
     * 批量移除当前租户成员。
     */
    R<Integer> deleteBatch(@Valid BatchDeleteIdentityUserCommand command);

    R<Boolean> updateStatus(@Valid UpdateIdentityUserStatusCommand command);

    R<Boolean> resetPassword(@Valid ResetIdentityUserPasswordCommand command);

    R<Boolean> unlock(@Valid UnlockIdentityUserCommand command);

    R<Boolean> requirePasswordReset(@Valid RequireIdentityUserPasswordResetCommand command);

    /**
     * 按用户名查询身份资料。
     */
    R<IdentityUserInfoVO> getUserInfo(@NotBlank String username);

    /**
     * 按用户 ID 查询身份资料。
     */
    R<IdentityUserInfoVO> getUserInfoById(@NotNull Long userId);

    /**
     * 按用户 ID 和用户名批量查询当前租户身份资料。
     */
    R<List<IdentityUserInfoVO>> listUserInfos(@Valid IdentityUserBatchRequest query);

    /**
     * 按接收目标解析身份用户资料。
     */
    R<List<IdentityUserInfoVO>> listUserInfosByTarget(@Valid IdentityUserTargetQuery query);

    /**
     * 绑定第三方登录身份。
     */
    R<ExternalIdentityBindingVO> bindExternalIdentity(@Valid BindExternalIdentityCommand command);

    /**
     * 解绑第三方登录身份。
     */
    R<Boolean> unbindExternalIdentity(@Valid UnbindExternalIdentityCommand command);

    /**
     * 查询第三方登录身份绑定。
     */
    R<ExternalIdentityBindingVO> findExternalIdentity(@Valid ExternalIdentityQuery query);

    /**
     * 查询成员的第三方登录身份绑定。
     */
    R<List<ExternalIdentityBindingVO>> listExternalIdentities(@NotNull Long userId);

    R<List<ExternalIdentityBindingVO>> listCurrentExternalIdentities();

    R<Boolean> unbindCurrentExternalIdentity(@Valid UnbindCurrentExternalIdentityCommand command);

}
