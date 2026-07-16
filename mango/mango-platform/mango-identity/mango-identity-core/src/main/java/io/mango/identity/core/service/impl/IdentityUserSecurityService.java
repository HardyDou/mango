package io.mango.identity.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.mango.common.result.Require;
import io.mango.identity.api.AuthIdentitySecurityProvider;
import io.mango.identity.api.command.ChangeRequiredPasswordCommand;
import io.mango.identity.api.enums.IdentityCode;
import io.mango.identity.api.vo.AuthUserVO;
import io.mango.identity.core.entity.IdentityUserEntity;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.service.IIdentityPasswordPolicyService;
import io.mango.identity.core.service.IIdentitySecurityPolicyService;
import io.mango.identity.core.service.IIdentityUserSecurityService;
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
public class IdentityUserSecurityService implements AuthIdentitySecurityProvider, IIdentityUserSecurityService {

    private static final String LOCK_REASON_TOO_MANY_FAILURES = "TOO_MANY_FAILED_LOGIN_ATTEMPTS";
    private final IdentityUserMapper identityUserMapper;
    private final IIdentitySecurityPolicyService policyService;
    private final IIdentityPasswordPolicyService passwordPolicyService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void assertLoginAllowed(AuthUserVO user) {
        if (user == null || user.getUserId() == null) {
            return;
        }
        LocalDateTime lockedUntil = user.getLockedUntil();
        if (lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now())) {
            Require.isTrue(false, IdentityCode.LOGIN_LOCKED, "账号已被临时锁定，请稍后再试或联系管理员");
        }
    }

    @Override
    @Transactional
    public void recordLoginFailure(Long userId) {
        if (!policyService.loginFailureLockEnabled()) {
            return;
        }
        IdentityUserEntity user = identityUserMapper.selectById(userId);
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
        identityUserMapper.update(null, new LambdaUpdateWrapper<IdentityUserEntity>()
                .eq(IdentityUserEntity::getId, userId)
                .set(IdentityUserEntity::getFailedLoginCount, nextFailures)
                .set(IdentityUserEntity::getLastFailedLoginAt, now)
                .set(IdentityUserEntity::getLockedUntil, user.getLockedUntil())
                .set(IdentityUserEntity::getLockedReason, user.getLockedReason())
                .set(IdentityUserEntity::getUpdateTime, now));
    }

    @Override
    @Transactional
    public void recordLoginSuccess(Long userId) {
        IdentityUserEntity user = identityUserMapper.selectById(userId);
        if (user == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        identityUserMapper.update(null, new LambdaUpdateWrapper<IdentityUserEntity>()
                .eq(IdentityUserEntity::getId, userId)
                .set(IdentityUserEntity::getFailedLoginCount, 0)
                .set(IdentityUserEntity::getLastFailedLoginAt, null)
                .set(IdentityUserEntity::getLockedUntil, null)
                .set(IdentityUserEntity::getLockedReason, null)
                .set(IdentityUserEntity::getLastLoginTime, now)
                .set(IdentityUserEntity::getUpdateTime, now));
    }

    @Override
    @Transactional
    public void changeRequiredPassword(ChangeRequiredPasswordCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "修改密码命令不能为空");
        Require.isTrue(Objects.equals(command.getNewPassword(), command.getConfirmPassword()),
                IdentityCode.VALIDATION_ERROR, "两次输入的新密码不一致");
        passwordPolicyService.validatePlainPassword(command.getNewPassword());
        IdentityUserEntity user = identityUserMapper.selectById(command.getUserId());
        Require.notNull(user, IdentityCode.NOT_FOUND, "用户不存在");
        LocalDateTime now = LocalDateTime.now();
        identityUserMapper.update(null, new LambdaUpdateWrapper<IdentityUserEntity>()
                .eq(IdentityUserEntity::getId, command.getUserId())
                .set(IdentityUserEntity::getPassword, passwordEncoder.encode(command.getNewPassword()))
                .set(IdentityUserEntity::getPasswordResetRequired, false)
                .set(IdentityUserEntity::getPasswordUpdatedAt, now)
                .set(IdentityUserEntity::getFailedLoginCount, 0)
                .set(IdentityUserEntity::getLastFailedLoginAt, null)
                .set(IdentityUserEntity::getLockedUntil, null)
                .set(IdentityUserEntity::getLockedReason, null)
                .set(IdentityUserEntity::getUpdateTime, now));
    }

    @Transactional
    public boolean unlock(Long userId) {
        IdentityUserEntity user = identityUserMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return identityUserMapper.update(null, new LambdaUpdateWrapper<IdentityUserEntity>()
                .eq(IdentityUserEntity::getId, userId)
                .set(IdentityUserEntity::getFailedLoginCount, 0)
                .set(IdentityUserEntity::getLastFailedLoginAt, null)
                .set(IdentityUserEntity::getLockedUntil, null)
                .set(IdentityUserEntity::getLockedReason, null)
                .set(IdentityUserEntity::getUpdateTime, now)) > 0;
    }

    @Transactional
    public boolean requirePasswordReset(Long userId) {
        IdentityUserEntity user = identityUserMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        return identityUserMapper.update(null, new LambdaUpdateWrapper<IdentityUserEntity>()
                .eq(IdentityUserEntity::getId, userId)
                .set(IdentityUserEntity::getPasswordResetRequired, true)
                .set(IdentityUserEntity::getUpdateTime, LocalDateTime.now())) > 0;
    }

    @Override
    public boolean isLocked(IdentityUserEntity user) {
        return user != null && user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now());
    }
}
