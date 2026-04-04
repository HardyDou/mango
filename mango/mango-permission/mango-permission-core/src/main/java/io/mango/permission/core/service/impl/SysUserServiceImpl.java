package io.mango.permission.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.permission.core.entity.SysMenu;
import io.mango.permission.core.entity.SysRole;
import io.mango.permission.core.entity.SysRoleMenu;
import io.mango.permission.core.entity.SysUser;
import io.mango.permission.core.entity.SysUserRole;
import io.mango.permission.api.vo.UserInfoVO;
import io.mango.permission.core.mapper.SysMenuMapper;
import io.mango.permission.core.mapper.SysRoleMapper;
import io.mango.permission.core.mapper.SysRoleMenuMapper;
import io.mango.permission.core.mapper.SysUserMapper;
import io.mango.permission.core.mapper.SysUserRoleMapper;
import io.mango.permission.core.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * System user service implementation
 *
 * @author Mango
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements ISysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysMenuMapper sysMenuMapper;

    @Override
    public UserInfoVO getUserInfo(String username) {
        SysUser user = getByUsername(username);
        if (user == null) {
            log.warn("User not found: {}", username);
            return null;
        }
        return buildUserInfoVO(user);
    }

    @Override
    public UserInfoVO getUserInfoById(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            log.warn("User not found by id: {}", userId);
            return null;
        }
        return buildUserInfoVO(user);
    }

    @Override
    public SysUser getByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        return sysUserMapper.selectOne(wrapper);
    }

    @Override
    public SysUser getById(Long userId) {
        return sysUserMapper.selectById(userId);
    }

    @Override
    public List<String> listUserPermissions(Long userId) {
        // 1. Get user roles
        List<Long> roleIds = listUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Get role-menu relationships
        LambdaQueryWrapper<SysRoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.in(SysRoleMenu::getRoleId, roleIds);
        List<Long> menuIds = sysRoleMenuMapper.selectList(roleMenuWrapper)
                .stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
        if (menuIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. Get menu permissions (menuType = 3 means button)
        LambdaQueryWrapper<SysMenu> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.in(SysMenu::getMenuId, menuIds)
                .eq(SysMenu::getMenuType, 3)  // button type
                .eq(SysMenu::getStatus, 1);   // enabled
        return sysMenuMapper.selectList(menuWrapper)
                .stream()
                .map(SysMenu::getMenuCode)
                .filter(code -> code != null && !code.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listUserRoles(Long userId) {
        List<Long> roleIds = listUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(SysRole::getRoleId, roleIds)
                .eq(SysRole::getStatus, 1);
        return sysRoleMapper.selectList(roleWrapper)
                .stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());
    }

    /**
     * Build UserInfoVO with permissions and roles
     */
    private UserInfoVO buildUserInfoVO(SysUser user) {
        UserInfoVO userInfo = new UserInfoVO();
        userInfo.setUserId(user.getUserId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setStatus(user.getStatus());

        // Load permissions and roles from database
        userInfo.setPermissions(listUserPermissions(user.getUserId()));
        userInfo.setRoles(listUserRoles(user.getUserId()));

        return userInfo;
    }

    /**
     * List user role IDs
     */
    private List<Long> listUserRoleIds(Long userId) {
        LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.eq(SysUserRole::getUserId, userId);
        return sysUserRoleMapper.selectList(userRoleWrapper)
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
    }
}
