package io.mango.rbac.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.rbac.api.vo.UserInfoVO;
import io.mango.rbac.core.entity.SysMenu;
import io.mango.rbac.core.entity.SysRole;
import io.mango.rbac.core.entity.SysRoleMenu;
import io.mango.rbac.core.entity.SysUser;
import io.mango.rbac.core.entity.SysUserRole;
import io.mango.rbac.core.mapper.SysMenuMapper;
import io.mango.rbac.core.mapper.SysRoleMapper;
import io.mango.rbac.core.mapper.SysRoleMenuMapper;
import io.mango.rbac.core.mapper.SysUserMapper;
import io.mango.rbac.core.mapper.SysUserRoleMapper;
import io.mango.rbac.core.service.ISysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SysUserServiceImpl
 *
 * @author Mango
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysUserServiceImpl Tests")
class SysUserServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Mock
    private SysMenuMapper sysMenuMapper;

    private SysUserServiceImpl sysUserService;

    @BeforeEach
    void setUp() {
        sysUserService = new SysUserServiceImpl(
                sysUserMapper, sysUserRoleMapper, sysRoleMapper, sysRoleMenuMapper, sysMenuMapper);
    }

    @Test
    @DisplayName("getByUsername should return user when exists")
    void getByUsername_existingUser_returnsUser() {
        SysUser user = createSysUser(1L, "testuser", "Test User");
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        SysUser result = sysUserService.getByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    @DisplayName("getByUsername should return null when user not found")
    void getByUsername_nonExistingUser_returnsNull() {
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        SysUser result = sysUserService.getByUsername("nonexistent");

        assertNull(result);
    }

    @Test
    @DisplayName("getById should return user when exists")
    void getById_existingUser_returnsUser() {
        SysUser user = createSysUser(1L, "testuser", "Test User");
        when(sysUserMapper.selectById(1L)).thenReturn(user);

        SysUser result = sysUserService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
    }

    @Test
    @DisplayName("getUserInfo should return null when user not found")
    void getUserInfo_nonExistingUser_returnsNull() {
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        UserInfoVO result = sysUserService.getUserInfo("nonexistent");

        assertNull(result);
    }

    @Test
    @DisplayName("getUserInfoById should return null when user not found")
    void getUserInfoById_nonExistingUser_returnsNull() {
        when(sysUserMapper.selectById(999L)).thenReturn(null);

        UserInfoVO result = sysUserService.getUserInfoById(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("listUserPermissions should return empty list when user has no roles")
    void listUserPermissions_noRoles_returnsEmptyList() {
        when(sysUserRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<String> result = sysUserService.listUserPermissions(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("listUserRoles should return empty list when user has no roles")
    void listUserRoles_noRoles_returnsEmptyList() {
        when(sysUserRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<String> result = sysUserService.listUserRoles(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("listUserRoles should return role codes when user has roles")
    void listUserRoles_withRoles_returnsRoleCodes() {
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(1L);
        userRole.setRoleId(1L);
        when(sysUserRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole));

        SysRole role = new SysRole();
        role.setRoleId(1L);
        role.setRoleCode("admin");
        role.setStatus(1);
        when(sysRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));

        List<String> result = sysUserService.listUserRoles(1L);

        assertEquals(1, result.size());
        assertEquals("admin", result.get(0));
    }

    @Test
    @DisplayName("SysUserServiceImpl implements ISysUserService")
    void implementsISysUserService() {
        assertTrue(sysUserService instanceof ISysUserService);
    }

    private SysUser createSysUser(Long userId, String username, String nickname) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setStatus(1);
        return user;
    }
}
