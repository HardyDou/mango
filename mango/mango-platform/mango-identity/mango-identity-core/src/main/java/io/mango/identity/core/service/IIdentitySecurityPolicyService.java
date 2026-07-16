package io.mango.identity.core.service;

public interface IIdentitySecurityPolicyService {

    boolean passwordComplexityEnabled();
    int passwordMinLength();
    boolean passwordRequireLetter();
    boolean passwordRequireDigit();
    boolean passwordRequireSpecialChar();
    boolean passwordAllowWhitespace();
    String passwordPattern();
    boolean resetRequiredAfterCreate();
    boolean resetRequiredAfterAdminReset();
    boolean loginFailureLockEnabled();
    int maxFailedAttempts();
    long failureWindowMinutes();
    long lockDurationMinutes();
}
