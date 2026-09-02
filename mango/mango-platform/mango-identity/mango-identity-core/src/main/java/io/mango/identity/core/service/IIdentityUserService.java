package io.mango.identity.core.service;

import io.mango.common.vo.PageResult;
import io.mango.identity.api.command.BindExternalIdentityCommand;
import io.mango.identity.api.command.BatchDeleteIdentityUserCommand;
import io.mango.identity.api.command.CreateIdentityUserCommand;
import io.mango.identity.api.command.ResetIdentityUserPasswordCommand;
import io.mango.identity.api.command.RequireIdentityUserPasswordResetCommand;
import io.mango.identity.api.command.UnbindExternalIdentityCommand;
import io.mango.identity.api.command.UnlockIdentityUserCommand;
import io.mango.identity.api.command.UpdateIdentityUserCommand;
import io.mango.identity.api.command.UpdateIdentityUserStatusCommand;
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
import io.mango.identity.core.entity.IdentityUserEntity;
import io.mango.identity.api.vo.IdentityUserInfoVO;
import io.mango.identity.api.vo.IdentityUserVO;
import io.mango.identity.api.vo.IdentityAccountAvailabilityVO;
import io.mango.identity.api.vo.ContactCaptchaTicketVO;
import io.mango.identity.api.vo.CurrentUserProfileVO;
import io.mango.infra.persistence.api.crud.MangoTypedCrudService;

import java.util.List;

/**
 * 身份用户服务接口。
 */
public interface IIdentityUserService extends MangoTypedCrudService<
        IdentityUserEntity, CreateIdentityUserCommand, UpdateIdentityUserCommand,
        IdentityUserPageQuery, IdentityUserVO, Long> {

    CurrentUserProfileVO currentProfile();

    CurrentUserProfileVO updateCurrentProfile(UpdateCurrentUserProfileCommand command);

    ContactCaptchaTicketVO sendCurrentContactCaptcha(SendContactCaptchaCommand command);

    CurrentUserProfileVO updateCurrentContact(UpdateCurrentUserContactCommand command);

    /**
     * 分页查询当前租户可管理的身份用户。
     */
    PageResult<IdentityUserVO> pageResult(IdentityUserPageQuery query);

    /**
     * 查询当前租户可管理的身份用户详情。
     */
    IdentityUserVO detail(Long userId);

    IdentityAccountAvailabilityVO accountAvailability(IdentityAccountAvailabilityQuery query);

    /**
     * 创建当前租户下的身份用户。
     */
    /**
     * 删除当前租户可管理的身份用户。
     */
    Boolean deleteUser(Long userId);

    /**
     * 批量删除当前租户可管理的身份用户。
     */
    Integer deleteBatch(BatchDeleteIdentityUserCommand command);

    /**
     * 修改当前租户可管理的身份用户状态。
     */
    Boolean updateStatus(UpdateIdentityUserStatusCommand command);

    /**
     * 重置当前租户可管理的身份用户密码。
     */
    Boolean resetPassword(ResetIdentityUserPasswordCommand command);

    /**
     * 解锁当前租户可管理的身份用户。
     */
    Boolean unlock(UnlockIdentityUserCommand command);

    /**
     * 要求当前租户可管理的身份用户下次登录改密。
     */
    Boolean requirePasswordReset(RequireIdentityUserPasswordResetCommand command);

    /**
     * 按用户名查询身份资料。
     */
    IdentityUserInfoVO getUserInfo(String username);

    /**
     * 按用户 ID 查询身份资料。
     */
    IdentityUserInfoVO getUserInfoById(Long userId);

    /**
     * 按用户 ID 和用户名批量查询当前租户身份资料。
     */
    List<IdentityUserInfoVO> listUserInfos(IdentityUserBatchRequest query);

    /**
     * 按接收目标解析身份资料列表。
     */
    List<IdentityUserInfoVO> listUserInfosByTarget(IdentityUserTargetQuery query);

    /**
     * 按用户名查询用户实体。
     */
    IdentityUserEntity getByUsername(String username);

    /**
     * 按登录域和用户名查询用户实体。
     */
    IdentityUserEntity getByUsername(String username, String realm);

    /**
     * 按用户 ID 查询用户实体。
     */
    IdentityUserEntity getById(Long userId);

    ExternalIdentityBindingVO bindExternalIdentity(BindExternalIdentityCommand command);

    Boolean unbindExternalIdentity(UnbindExternalIdentityCommand command);

    ExternalIdentityBindingVO findExternalIdentity(ExternalIdentityQuery query);

    List<ExternalIdentityBindingVO> listExternalIdentities(Long userId);

    List<ExternalIdentityBindingVO> listCurrentExternalIdentities();

    Boolean unbindCurrentExternalIdentity(UnbindCurrentExternalIdentityCommand command);

}
