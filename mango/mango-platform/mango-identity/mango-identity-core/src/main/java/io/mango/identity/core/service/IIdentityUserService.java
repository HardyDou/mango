package io.mango.identity.core.service;

import io.mango.common.vo.PageResult;
import io.mango.identity.api.command.BindExternalIdentityCommand;
import io.mango.identity.api.command.CreateIdentityUserCommand;
import io.mango.identity.api.command.ResetIdentityUserPasswordCommand;
import io.mango.identity.api.command.RequireIdentityUserPasswordResetCommand;
import io.mango.identity.api.command.UnbindExternalIdentityCommand;
import io.mango.identity.api.command.UnlockIdentityUserCommand;
import io.mango.identity.api.command.UpdateIdentityUserCommand;
import io.mango.identity.api.command.UpdateIdentityUserStatusCommand;
import io.mango.identity.api.query.ExternalIdentityQuery;
import io.mango.identity.api.query.IdentityUserPageQuery;
import io.mango.identity.api.query.IdentityUserTargetQuery;
import io.mango.identity.api.vo.ExternalIdentityBindingVO;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.api.vo.IdentityUserInfo;
import io.mango.identity.api.vo.IdentityUserVO;

import java.util.List;

/**
 * 身份用户服务接口。
 */
public interface IIdentityUserService {

    /**
     * 分页查询当前租户可管理的身份用户。
     */
    PageResult<IdentityUserVO> page(IdentityUserPageQuery query);

    /**
     * 查询当前租户可管理的身份用户详情。
     */
    IdentityUserVO detail(Long userId);

    /**
     * 创建当前租户下的身份用户。
     */
    Long create(CreateIdentityUserCommand command);

    /**
     * 更新当前租户可管理的身份用户。
     */
    Boolean update(UpdateIdentityUserCommand command);

    /**
     * 删除当前租户可管理的身份用户。
     */
    Boolean delete(Long userId);

    /**
     * 批量删除当前租户可管理的身份用户。
     */
    Integer deleteBatch(List<Long> userIds);

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
    IdentityUserInfo getUserInfo(String username);

    /**
     * 按用户 ID 查询身份资料。
     */
    IdentityUserInfo getUserInfoById(Long userId);

    /**
     * 按接收目标解析身份资料列表。
     */
    List<IdentityUserInfo> listUserInfosByTarget(IdentityUserTargetQuery query);

    /**
     * 按用户名查询用户实体。
     */
    IdentityUser getByUsername(String username);

    /**
     * 按登录域和用户名查询用户实体。
     */
    IdentityUser getByUsername(String username, String realm);

    /**
     * 按用户 ID 查询用户实体。
     */
    IdentityUser getById(Long userId);

    ExternalIdentityBindingVO bindExternalIdentity(BindExternalIdentityCommand command);

    Boolean unbindExternalIdentity(UnbindExternalIdentityCommand command);

    ExternalIdentityBindingVO findExternalIdentity(ExternalIdentityQuery query);

    List<ExternalIdentityBindingVO> listExternalIdentities(Long userId);

}
