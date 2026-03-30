package io.mango.permission.core.controller;

import io.mango.common.result.R;
import io.mango.permission.api.vo.UserInfoVO;
import io.mango.permission.core.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * System user controller
 *
 * @author Mango
 */
@RestController
@RequestMapping("/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    /**
     * Get current user info with permissions
     */
    @GetMapping("/info")
    public R<UserInfoVO> info() {
        // In real implementation, get username from security context
        String username = "admin";
        return R.ok(sysUserService.getUserInfo(username));
    }

    /**
     * Get user info by user ID
     */
    @GetMapping("/info/{userId}")
    public R<UserInfoVO> infoById(Long userId) {
        return R.ok(sysUserService.getUserInfoById(userId));
    }
}
