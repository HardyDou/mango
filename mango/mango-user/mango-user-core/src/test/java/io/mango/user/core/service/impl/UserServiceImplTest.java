package io.mango.user.core.service.impl;

import io.mango.user.api.po.User;
import io.mango.user.core.mapper.UserMapper;
import io.mango.user.core.service.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserServiceImpl
 *
 * @author Mango
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Tests")
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userMapper);
    }

    @Test
    @DisplayName("getById should return user when exists")
    void getById_existingUser_returnsUser() {
        User user = createUser(1L, "Test User");
        when(userMapper.selectById(1L)).thenReturn(user);

        User result = userService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("Test User", result.getRealName());
    }

    @Test
    @DisplayName("getById should return null when user not found")
    void getById_nonExistingUser_returnsNull() {
        when(userMapper.selectById(999L)).thenReturn(null);

        User result = userService.getById(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("create should return false when userId is null")
    void create_nullUserId_returnsFalse() {
        User user = new User();
        user.setRealName("Test User");

        boolean result = userService.create(user);

        assertFalse(result);
    }

    @Test
    @DisplayName("create should return true when user is valid")
    void create_validUser_returnsTrue() {
        User user = createUser(1L, "Test User");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        boolean result = userService.create(user);

        assertTrue(result);
        verify(userMapper).insert(any(User.class));
    }

    @Test
    @DisplayName("update should return false when userId is null")
    void update_nullUserId_returnsFalse() {
        User user = new User();

        boolean result = userService.update(user);

        assertFalse(result);
    }

    @Test
    @DisplayName("update should return true when user is valid")
    void update_validUser_returnsTrue() {
        User user = createUser(1L, "Test User");
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        boolean result = userService.update(user);

        assertTrue(result);
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    @DisplayName("delete should return true when user is deleted")
    void delete_existingUser_returnsTrue() {
        when(userMapper.deleteById(1L)).thenReturn(1);

        boolean result = userService.delete(1L);

        assertTrue(result);
    }

    @Test
    @DisplayName("delete should return false when user not found")
    void delete_nonExistingUser_returnsFalse() {
        when(userMapper.deleteById(999L)).thenReturn(0);

        boolean result = userService.delete(999L);

        assertFalse(result);
    }

    @Test
    @DisplayName("UserServiceImpl implements IUserService")
    void implementsIUserService() {
        assertTrue(userService instanceof IUserService);
    }

    private User createUser(Long userId, String realName) {
        User user = new User();
        user.setUserId(userId);
        user.setRealName(realName);
        user.setUserType(1);
        return user;
    }
}
