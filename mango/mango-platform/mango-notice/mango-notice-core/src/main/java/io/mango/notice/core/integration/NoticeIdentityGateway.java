package io.mango.notice.core.integration;

import io.mango.common.vo.PageResult;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.command.BindExternalIdentityCommand;
import io.mango.identity.api.command.CreateIdentityUserCommand;
import io.mango.identity.api.command.UpdateIdentityUserCommand;
import io.mango.identity.api.query.IdentityUserPageQuery;
import io.mango.identity.api.vo.ExternalIdentityBindingVO;
import io.mango.identity.api.vo.IdentityUserInfo;
import io.mango.identity.api.vo.IdentityUserInfoVO;
import io.mango.identity.api.vo.IdentityUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Isolates the Notice domain from the remote {@code R<T>} transport envelope. */
@Component
@RequiredArgsConstructor
public class NoticeIdentityGateway {

    private final IdentityUserApi identityUserApi;

    public NoticeRemoteResult<PageResult<IdentityUserVO>> page(IdentityUserPageQuery query) {
        return NoticeRemoteResult.from(identityUserApi.page(query));
    }

    public NoticeRemoteResult<IdentityUserVO> detail(Long userId) {
        return NoticeRemoteResult.from(identityUserApi.detail(userId));
    }

    public NoticeRemoteResult<Long> create(CreateIdentityUserCommand command) {
        return NoticeRemoteResult.from(identityUserApi.create(command));
    }

    public NoticeRemoteResult<Boolean> update(UpdateIdentityUserCommand command) {
        return NoticeRemoteResult.from(identityUserApi.update(command));
    }

    public NoticeRemoteResult<IdentityUserInfo> getUserInfoById(Long userId) {
        return NoticeRemoteResult.from(identityUserApi.getUserInfoById(userId))
                .map(NoticeIdentityGateway::asLegacyUserInfo);
    }

    public NoticeRemoteResult<ExternalIdentityBindingVO> bindExternalIdentity(
            BindExternalIdentityCommand command) {
        return NoticeRemoteResult.from(identityUserApi.bindExternalIdentity(command));
    }

    private static IdentityUserInfo asLegacyUserInfo(IdentityUserInfoVO userInfo) {
        return userInfo;
    }
}
