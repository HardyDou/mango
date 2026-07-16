package io.mango.identity.core.service.impl;

import io.mango.identity.core.adapter.SysConfigValueAdapter;
import io.mango.identity.core.service.IIdentitySecurityPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 身份安全策略读取服务。优先使用系统参数，缺省时回退到 Spring 配置默认值。
 */
@Service
@RequiredArgsConstructor
public class IdentitySecurityPolicyService implements IIdentitySecurityPolicyService {

    private static final String PASSWORD_COMPLEXITY_ENABLED = "identity.security.password-complexity.enabled";
    private static final String PASSWORD_MIN_LENGTH = "identity.security.password.min-length";
    private static final String PASSWORD_REQUIRE_LETTER = "identity.security.password.require-letter";
    private static final String PASSWORD_REQUIRE_DIGIT = "identity.security.password.require-digit";
    private static final String PASSWORD_REQUIRE_SPECIAL_CHAR = "identity.security.password.require-special-char";
    private static final String PASSWORD_ALLOW_WHITESPACE = "identity.security.password.allow-whitespace";
    private static final String PASSWORD_PATTERN = "identity.security.password.pattern";
    private static final String RESET_AFTER_CREATE_ENABLED = "identity.security.password-reset-after-create.enabled";
    private static final String RESET_AFTER_ADMIN_RESET_ENABLED = "identity.security.password-reset-after-admin-reset.enabled";
    private static final String LOGIN_FAILURE_LOCK_ENABLED = "identity.security.login-failure-lock.enabled";
    private static final String LOGIN_MAX_FAILED_ATTEMPTS = "sys.login.lockCount";
    private static final String LOGIN_FAILURE_WINDOW_MINUTES = "identity.security.login.failure-window-minutes";
    private static final String LOGIN_LOCK_DURATION_MINUTES = "identity.security.login.lock-duration-minutes";

    private final IdentitySecurityProperties properties;
    private final SysConfigValueAdapter configAdapter;

    public boolean passwordComplexityEnabled() {
        return booleanConfig(PASSWORD_COMPLEXITY_ENABLED, properties.getPassword().isComplexityEnabled());
    }

    public int passwordMinLength() {
        return integerConfig(PASSWORD_MIN_LENGTH, properties.getPassword().getMinLength());
    }

    public boolean passwordRequireLetter() {
        return booleanConfig(PASSWORD_REQUIRE_LETTER, properties.getPassword().isRequireLetter());
    }

    public boolean passwordRequireDigit() {
        return booleanConfig(PASSWORD_REQUIRE_DIGIT, properties.getPassword().isRequireDigit());
    }

    public boolean passwordRequireSpecialChar() {
        return booleanConfig(PASSWORD_REQUIRE_SPECIAL_CHAR, properties.getPassword().isRequireSpecialChar());
    }

    public boolean passwordAllowWhitespace() {
        return booleanConfig(PASSWORD_ALLOW_WHITESPACE, properties.getPassword().isAllowWhitespace());
    }

    public String passwordPattern() {
        return stringConfig(PASSWORD_PATTERN, properties.getPassword().getPattern());
    }

    public boolean resetRequiredAfterCreate() {
        return booleanConfig(RESET_AFTER_CREATE_ENABLED, properties.getPassword().isResetRequiredAfterCreate());
    }

    public boolean resetRequiredAfterAdminReset() {
        return booleanConfig(RESET_AFTER_ADMIN_RESET_ENABLED, properties.getPassword().isResetRequiredAfterAdminReset());
    }

    public boolean loginFailureLockEnabled() {
        return booleanConfig(LOGIN_FAILURE_LOCK_ENABLED, properties.getLogin().isFailureLockEnabled());
    }

    public int maxFailedAttempts() {
        return integerConfig(LOGIN_MAX_FAILED_ATTEMPTS, properties.getLogin().getMaxFailedAttempts());
    }

    public long failureWindowMinutes() {
        return integerConfig(LOGIN_FAILURE_WINDOW_MINUTES, Math.toIntExact(properties.getLogin().getFailureWindowMinutes()));
    }

    public long lockDurationMinutes() {
        return integerConfig(LOGIN_LOCK_DURATION_MINUTES, Math.toIntExact(properties.getLogin().getLockDurationMinutes()));
    }

    private boolean booleanConfig(String key, boolean defaultValue) {
        return configAdapter.booleanValue(key, defaultValue);
    }

    private int integerConfig(String key, int defaultValue) {
        return configAdapter.integerValue(key, defaultValue);
    }

    private String stringConfig(String key, String defaultValue) {
        return configAdapter.stringValue(key, defaultValue);
    }
}
