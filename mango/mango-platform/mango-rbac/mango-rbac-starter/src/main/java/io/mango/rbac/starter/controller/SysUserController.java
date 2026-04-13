package io.mango.rbac.starter.controller;

import io.mango.common.result.R;
import io.mango.infra.security.api.ITokenService;
import io.mango.rbac.api.po.SysUser;
import io.mango.rbac.api.SysUserApi;
import io.mango.rbac.api.vo.UserInfoVO;
import io.mango.rbac.core.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * System user controller - implements SysUserApi
 *
 * @author Mango
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class SysUserController implements SysUserApi {

    private final ISysUserService sysUserService;
    private final ITokenService tokenService;

    @Override
    public UserInfoVO getUserInfo(String username) {
        return sysUserService.getUserInfo(username);
    }

    @Override
    public UserInfoVO getUserInfoById(Long userId) {
        return sysUserService.getUserInfoById(userId);
    }

    @Override
    public SysUser getByUsername(String username) {
        io.mango.rbac.core.entity.SysUser user = sysUserService.getByUsername(username);
        if (user != null) {
            user.setPassword(null); // Remove password before returning
        }
        return convertToApiPo(user);
    }

    @Override
    public SysUser getById(Long userId) {
        io.mango.rbac.core.entity.SysUser user = sysUserService.getById(userId);
        if (user != null) {
            user.setPassword(null); // Remove password before returning
        }
        return convertToApiPo(user);
    }

    @Override
    public SysUser getByUsernameForAuth(String username) {
        // Authentication purpose - return user with password intact
        // This method should ONLY be called by the auth module
        return convertToApiPo(sysUserService.getByUsername(username));
    }

    @Override
    public SysUser getByIdForAuth(Long userId) {
        // Authentication purpose - return user with password intact
        // This method should ONLY be called by the auth module
        return convertToApiPo(sysUserService.getById(userId));
    }

    /**
     * Get current user info with permissions (HTTP endpoint)
     * Gets username from Authorization header
     */
    @GetMapping("/info")
    public R<UserInfoVO> info(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String username = getUsernameFromToken(authHeader);
        if (username == null) {
            return R.fail(401, "Not authenticated");
        }
        UserInfoVO userInfo = getUserInfo(username);
        if (userInfo == null) {
            return R.fail(404, "User not found");
        }
        return R.ok(userInfo);
    }

    /**
     * Get username from JWT token in Authorization header
     */
    private String getUsernameFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authHeader.substring(7);
            return tokenService.getUsername(token);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get user info by user ID (HTTP endpoint)
     */
    @GetMapping("/info/{userId}")
    public R<UserInfoVO> infoById(@PathVariable(value = "userId") Long userId) {
        UserInfoVO userInfo = getUserInfoById(userId);
        if (userInfo == null) {
            return R.fail(404, "User not found");
        }
        return R.ok(userInfo);
    }

    /**
     * Get user entity by username (HTTP endpoint)
     */
    @GetMapping("/{username}")
    public R<SysUser> getByUsernameEndpoint(@PathVariable(value = "username") String username) {
        SysUser user = getByUsername(username);
        if (user == null) {
            return R.fail(404, "User not found");
        }
        return R.ok(user);
    }

    /**
     * Get user entity by user ID (HTTP endpoint)
     */
    @GetMapping("/id/{userId}")
    public R<SysUser> getByIdEndpoint(@PathVariable(value = "userId") Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            return R.fail(404, "User not found");
        }
        return R.ok(user);
    }

    /**
     * Convert core entity to API PO
     */
    private SysUser convertToApiPo(io.mango.rbac.core.entity.SysUser entity) {
        if (entity == null) {
            return null;
        }
        SysUser po = new SysUser();
        po.setUserId(entity.getUserId());
        po.setUsername(entity.getUsername());
        po.setNickname(entity.getNickname());
        po.setEmail(entity.getEmail());
        po.setPhone(entity.getPhone());
        po.setAvatar(entity.getAvatar());
        po.setStatus(entity.getStatus());
        po.setCreateTime(entity.getCreateTime());
        po.setUpdateTime(entity.getUpdateTime());
        po.setLastLoginTime(entity.getLastLoginTime());
        po.setRemark(entity.getRemark());
        // Password is NOT copied for security
        return po;
    }
}
