package io.mango.identity.core.service.impl;

import io.mango.common.exception.BizException;
import io.mango.identity.api.AuthIdentitySecurityProvider;
import io.mango.identity.api.command.ChangeRequiredPasswordCommand;
import io.mango.identity.api.vo.AuthUserInfo;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.mapper.IdentityUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 身份用户安全状态服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityUserSecurityService implements AuthIdentitySecurityProvider {

    private static final String LOCK_REASON_TOO_MANY_FAILURES = "TOO_MANY_FAILED_LOGIN_ATTEMPTS";
    private static final int LOGIN_ATTEMPT_LOCKED_CODE = 1429;

    private final IdentityUserMapper identityUserMapper;
    private final IdentitySecurityPolicyService policyService;
    private final IdentityPasswordPolicyService passwordPolicyService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void assertLoginAllowed(AuthUserInfo user) {
        if (user == null || user.getUserId() == null) {
            return;
        }
        LocalDateTime lockedUntil = user.getLockedUntil();
        if (lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now())) {
            throw new BizException(LOGIN_ATTEMPT_LOCKED_CODE, "账号已被临时锁定，请稍后再试或联系管理员");
        }
    }

    @Override
    @Transactional
    public void recordLoginFailure(Long userId) {
        if (!policyService.loginFailureLockEnabled()) {
            return;
        }
        IdentityUser user = identityUserMapper.selectById(userId);
        if (user == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int currentFailures = user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount();
        if (user.getLockedUntil() != null && !user.getLockedUntil().isAfter(now)) {
            currentFailures = 0;
            user.setLockedUntil(null);
            user.setLockedReason(null);
        }
        LocalDateTime lastFailedLoginAt = user.getLastFailedLoginAt();
        if (lastFailedLoginAt == null || lastFailedLoginAt.plusMinutes(policyService.failureWindowMinutes()).isBefore(now)) {
            currentFailures = 0;
        }
        int nextFailures = currentFailures + 1;
        user.setFailedLoginCount(nextFailures);
        user.setLastFailedLoginAt(now);
        if (nextFailures >= policyService.maxFailedAttempts()) {
            user.setLockedUntil(now.plusMinutes(policyService.lockDurationMinutes()));
            user.setLockedReason(LOCK_REASON_TOO_MANY_FAILURES);
            log.warn("Identity user locked after failed logins: userId={}, failedCount={}", userId, nextFailures);
        }
        user.setUpdateTime(now);
        identityUserMapper.updateById(user);
    }

    @Override
    @Transactional
    public void recordLoginSuccess(Long userId) {
        IdentityUser user = identityUserMapper.selectById(userId);
        if (user == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        user.setFailedLoginCount(0);
        user.setLastFailedLoginAt(null);
        user.setLockedUntil(null);
        user.setLockedReason(null);
        user.setLastLoginTime(now);
        user.setUpdateTime(now);
        identityUserMapper.updateById(user);
    }

    @Override
    @Transactional
    public void changeRequiredPassword(ChangeRequiredPasswordCommand command) {
        if (!Objects.equals(command.getNewPassword(), command.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的新密码不一致");
        }
        passwordPolicyService.validatePlainPassword(command.getNewPassword());
        IdentityUser user = identityUserMapper.selectById(command.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        user.setPassword(passwordEncoder.encode(command.getNewPassword()));
        user.setPasswordResetRequired(false);
        user.setPasswordUpdatedAt(now);
        user.setFailedLoginCount(0);
        user.setLastFailedLoginAt(null);
        user.setLockedUntil(null);
        user.setLockedReason(null);
        user.setUpdateTime(now);
        identityUserMapper.updateById(user);
    }

    @Transactional
    public boolean unlock(Long userId) {
        IdentityUser user = identityUserMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        user.setFailedLoginCount(0);
        user.setLastFailedLoginAt(null);
        user.setLockedUntil(null);
        user.setLockedReason(null);
        user.setUpdateTime(LocalDateTime.now());
        return identityUserMapper.updateById(user) > 0;
    }

    @Transactional
    public boolean requirePasswordReset(Long userId) {
        IdentityUser user = identityUserMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        user.setPasswordResetRequired(true);
        user.setUpdateTime(LocalDateTime.now());
        return identityUserMapper.updateById(user) > 0;
    }

    boolean isLocked(IdentityUser user) {
        return user != null && user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now());
    }
}
