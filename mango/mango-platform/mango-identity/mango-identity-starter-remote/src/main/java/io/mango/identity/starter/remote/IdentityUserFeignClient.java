package io.mango.identity.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.identity.api.IdentityUserApi;
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
@FeignClient(name = "mango-identity", path = "/identity")
public interface IdentityUserFeignClient extends IdentityUserApi {

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
    @PutMapping("/users")
    R<Boolean> update(@RequestBody UpdateIdentityUserCommand command);

    @Override
    @DeleteMapping("/users")
    R<Boolean> delete(@RequestParam("userId") Long userId);

    @Override
    @PostMapping("/users/delete-batch")
    R<Integer> deleteBatch(@RequestBody BatchDeleteIdentityUserCommand command);

    @Override
    @GetMapping("/user/info/username")
    R<IdentityUserInfo> getUserInfo(@RequestParam("username") String username);

    @Override
    @GetMapping("/user/info/id")
    R<IdentityUserInfo> getUserInfoById(@RequestParam("userId") Long userId);

    @Override
    @GetMapping("/user/info/targets")
    R<List<IdentityUserInfo>> listUserInfosByTarget(@SpringQueryMap IdentityUserTargetQuery query);

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

}
