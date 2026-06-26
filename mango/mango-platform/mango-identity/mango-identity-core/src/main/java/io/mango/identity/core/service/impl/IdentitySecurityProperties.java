package io.mango.identity.core.service.impl;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 身份安全策略配置。
 */
@Data
@ConfigurationProperties(prefix = "mango.identity.security")
public class IdentitySecurityProperties {

    private Password password = new Password();

    private Login login = new Login();

    @Data
    public static class Password {
        private boolean complexityEnabled = true;
        private int minLength = 8;
        private boolean requireLetter = true;
        private boolean requireDigit = true;
        private boolean requireSpecialChar = false;
        private boolean allowWhitespace = false;
        private String pattern;
        private boolean resetRequiredAfterCreate = true;
        private boolean resetRequiredAfterAdminReset = true;
    }

    @Data
    public static class Login {
        private boolean failureLockEnabled = true;
        private int maxFailedAttempts = 5;
        private long failureWindowMinutes = 60;
        private long lockDurationMinutes = 15;
    }
}
