package io.mango.permission.core.service.impl;

import io.mango.permission.api.vo.UserInfoVO;
import io.mango.permission.core.service.SysUserService;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * System user service implementation
 *
 * @author Mango
 */
@Service
public class SysUserServiceImpl implements SysUserService {

    @Override
    public UserInfoVO getUserInfo(String username) {
        // TODO: Implement actual user and permission query from database
        // This is a placeholder implementation returning mock data

        UserInfoVO userInfo = new UserInfoVO();
        userInfo.setUserId(1L);
        userInfo.setUsername(username);
        userInfo.setNickname("Admin");
        userInfo.setEmail("admin@example.com");
        userInfo.setPhone("13800138000");
        userInfo.setStatus(1);

        // Mock permissions - in real implementation, query from sys_role_menu + sys_menu
        userInfo.setPermissions(Arrays.asList(
                "system:user:view",
                "system:user:add",
                "system:user:edit",
                "system:user:del",
                "system:role:view",
                "system:role:add",
                "system:role:edit",
                "system:role:del"
        ));

        userInfo.setRoles(Arrays.asList("admin", "developer"));

        return userInfo;
    }

    @Override
    public UserInfoVO getUserInfoById(Long userId) {
        // TODO: Implement actual user query by ID
        UserInfoVO userInfo = new UserInfoVO();
        userInfo.setUserId(userId);
        userInfo.setUsername("user_" + userId);
        userInfo.setNickname("User " + userId);
        userInfo.setStatus(1);
        userInfo.setPermissions(Arrays.asList("system:user:view"));
        return userInfo;
    }
}
