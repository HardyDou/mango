package io.mango.identity.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.identity.api.IdentityUserApi;
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
import io.mango.identity.api.query.IdentityUserTargetQuery;
import io.mango.identity.api.request.IdentityUserBatchRequest;
import io.mango.identity.api.vo.ExternalIdentityBindingVO;
import io.mango.identity.api.vo.IdentityUserInfoVO;
import io.mango.identity.api.vo.IdentityUserVO;
import io.mango.identity.api.vo.IdentityAccountAvailabilityVO;
import io.mango.identity.api.vo.ContactCaptchaTicketVO;
import io.mango.identity.api.vo.CurrentUserProfileVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 身份用户资料 Feign 客户端。
 */
@FeignClient(name = "mango-identity", contextId = "identityUserFeignClient", path = "/identity")
public interface IdentityUserFeignClient extends IdentityUserApi {

    @Override
    @GetMapping("/me/profile")
    R<CurrentUserProfileVO> currentProfile();

    @Override
    @PutMapping("/me/profile")
    R<CurrentUserProfileVO> updateCurrentProfile(@RequestBody UpdateCurrentUserProfileCommand command);

    @Override
    @PostMapping("/me/contact-captcha")
    R<ContactCaptchaTicketVO> sendCurrentContactCaptcha(@RequestBody SendContactCaptchaCommand command);

    @Override
    @PutMapping("/me/contact")
    R<CurrentUserProfileVO> updateCurrentContact(@RequestBody UpdateCurrentUserContactCommand command);

    @Override
    @GetMapping("/users/page")
    R<PageResult<IdentityUserVO>> page(@SpringQueryMap IdentityUserPageQuery query);

    @Override
    @GetMapping("/users/detail")
    R<IdentityUserVO> detail(@RequestParam("userId") Long userId);

    @Override
    @PostMapping("/users")
    R<Long> create(@RequestBody CreateIdentityUserCommand command);

    @Override
    @GetMapping("/users/account-availability")
    R<IdentityAccountAvailabilityVO> accountAvailability(@SpringQueryMap IdentityAccountAvailabilityQuery query);

    @Override
    @PutMapping("/users")
    R<Boolean> update(@RequestBody UpdateIdentityUserCommand command);

    @Override
    @DeleteMapping("/users")
    R<Boolean> delete(@RequestParam("userId") Long userId);

    @Override
    @PostMapping("/users/delete-batch")
    R<Integer> deleteBatch(@RequestBody BatchDeleteIdentityUserCommand command);

    @Override
    @PutMapping("/users/status")
    R<Boolean> updateStatus(@RequestBody UpdateIdentityUserStatusCommand command);

    @Override
    @PutMapping("/users/password/reset")
    R<Boolean> resetPassword(@RequestBody ResetIdentityUserPasswordCommand command);

    @Override
    @PutMapping("/users/unlock")
    R<Boolean> unlock(@RequestBody UnlockIdentityUserCommand command);

    @Override
    @PutMapping("/users/password/reset-required")
    R<Boolean> requirePasswordReset(@RequestBody RequireIdentityUserPasswordResetCommand command);

    @Override
    @GetMapping("/user/info/username")
    R<IdentityUserInfoVO> getUserInfo(@RequestParam("username") String username);

    @Override
    @GetMapping("/user/info/id")
    R<IdentityUserInfoVO> getUserInfoById(@RequestParam("userId") Long userId);

    @Override
    @PostMapping("/user/info/batch")
    R<List<IdentityUserInfoVO>> listUserInfos(@RequestBody IdentityUserBatchRequest query);

    @Override
    @GetMapping("/user/info/targets")
    R<List<IdentityUserInfoVO>> listUserInfosByTarget(@SpringQueryMap IdentityUserTargetQuery query);

    @Override
    @PostMapping("/users/external-identities")
    R<ExternalIdentityBindingVO> bindExternalIdentity(@RequestBody BindExternalIdentityCommand command);

    @Override
    @DeleteMapping("/users/external-identities")
    R<Boolean> unbindExternalIdentity(@RequestBody UnbindExternalIdentityCommand command);

    @Override
    @GetMapping("/users/external-identity")
    R<ExternalIdentityBindingVO> findExternalIdentity(@SpringQueryMap ExternalIdentityQuery query);

    @Override
    @GetMapping("/users/external-identities")
    R<List<ExternalIdentityBindingVO>> listExternalIdentities(@RequestParam("userId") Long userId);

    @Override
    @GetMapping("/me/external-identities")
    R<List<ExternalIdentityBindingVO>> listCurrentExternalIdentities();

    @Override
    @DeleteMapping("/me/external-identities")
    R<Boolean> unbindCurrentExternalIdentity(@RequestBody UnbindCurrentExternalIdentityCommand command);

}
