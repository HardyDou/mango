package io.mango.identity.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.identity.api.command.BatchDeleteIdentityUserCommand;
import io.mango.identity.api.command.BindExternalIdentityCommand;
import io.mango.identity.api.command.CreateIdentityUserCommand;
import io.mango.identity.api.command.UnbindExternalIdentityCommand;
import io.mango.identity.api.command.UpdateIdentityUserCommand;
import io.mango.identity.api.query.ExternalIdentityQuery;
import io.mango.identity.api.query.IdentityUserPageQuery;
import io.mango.identity.api.query.IdentityUserTargetQuery;
import io.mango.identity.api.vo.ExternalIdentityBindingVO;
import io.mango.identity.api.vo.IdentityUserInfo;
import io.mango.identity.api.vo.IdentityUserVO;

import java.util.List;

/**
 * 身份用户资料 HTTP 契约。
 */
public interface IdentityUserApi {

    /**
     * 分页查询当前租户成员。
     */
    R<PageResult<IdentityUserVO>> page(IdentityUserPageQuery query);

    /**
     * Query current tenant member detail by user ID.
     */
    R<IdentityUserVO> detail(Long userId);

    /**
     * 创建当前租户成员。
     */
    R<Long> create(CreateIdentityUserCommand command);

    /**
     * 更新当前租户成员。
     */
    R<Boolean> update(UpdateIdentityUserCommand command);

    /**
     * 移除当前租户成员。
     */
    R<Boolean> delete(Long userId);

    /**
     * 批量移除当前租户成员。
     */
    R<Integer> deleteBatch(BatchDeleteIdentityUserCommand command);

    /**
     * 按用户名查询身份资料。
     */
    R<IdentityUserInfo> getUserInfo(String username);

    /**
     * 按用户 ID 查询身份资料。
     */
    R<IdentityUserInfo> getUserInfoById(Long userId);

    /**
     * 按接收目标解析身份用户资料。
     */
    R<List<IdentityUserInfo>> listUserInfosByTarget(IdentityUserTargetQuery query);

    /**
     * 绑定第三方登录身份。
     */
    R<ExternalIdentityBindingVO> bindExternalIdentity(BindExternalIdentityCommand command);

    /**
     * 解绑第三方登录身份。
     */
    R<Boolean> unbindExternalIdentity(UnbindExternalIdentityCommand command);

    /**
     * 查询第三方登录身份绑定。
     */
    R<ExternalIdentityBindingVO> findExternalIdentity(ExternalIdentityQuery query);

    /**
     * 查询成员的第三方登录身份绑定。
     */
    R<List<ExternalIdentityBindingVO>> listExternalIdentities(Long userId);

}
