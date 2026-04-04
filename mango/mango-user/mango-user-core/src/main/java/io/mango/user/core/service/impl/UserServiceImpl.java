package io.mango.user.core.service.impl;

import io.mango.user.api.po.User;
import io.mango.user.core.mapper.UserMapper;
import io.mango.user.core.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * User service implementation
 *
 * @author Mango
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserMapper userMapper;

    @Override
    public User getById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public boolean create(User user) {
        if (user.getUserId() == null) {
            log.warn("User ID is required for creation");
            return false;
        }
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        return userMapper.insert(user) > 0;
    }

    @Override
    public boolean update(User user) {
        if (user.getUserId() == null) {
            log.warn("User ID is required for update");
            return false;
        }
        user.setUpdateTime(LocalDateTime.now());
        return userMapper.updateById(user) > 0;
    }

    @Override
    public boolean delete(Long userId) {
        return userMapper.deleteById(userId) > 0;
    }
}
