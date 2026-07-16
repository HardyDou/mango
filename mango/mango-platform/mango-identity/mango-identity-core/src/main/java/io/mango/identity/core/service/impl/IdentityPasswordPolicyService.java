package io.mango.identity.core.service.impl;

import io.mango.common.result.Require;
import io.mango.identity.api.enums.IdentityCode;
import io.mango.identity.core.service.IIdentityPasswordPolicyService;
import io.mango.identity.core.service.IIdentitySecurityPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 身份密码策略校验服务。
 */
@Service
@RequiredArgsConstructor
public class IdentityPasswordPolicyService implements IIdentityPasswordPolicyService {

    private final IIdentitySecurityPolicyService policyService;

    /**
     * 校验明文密码是否满足安全基线。
     */
    @Override
    public void validatePlainPassword(String password) {
        if (!policyService.passwordComplexityEnabled()) {
            return;
        }
        Require.notBlank(password, IdentityCode.VALIDATION_ERROR, "密码不能为空");
        Require.isTrue(password.length() >= policyService.passwordMinLength(), IdentityCode.VALIDATION_ERROR,
                "密码长度至少" + policyService.passwordMinLength() + "位");
        Require.isTrue(policyService.passwordAllowWhitespace()
                        || password.chars().noneMatch(Character::isWhitespace),
                IdentityCode.VALIDATION_ERROR, "密码不能包含空白字符");
        Require.isTrue(!policyService.passwordRequireLetter() || password.chars().anyMatch(Character::isLetter),
                IdentityCode.VALIDATION_ERROR, "密码必须包含字母");
        Require.isTrue(!policyService.passwordRequireDigit() || password.chars().anyMatch(Character::isDigit),
                IdentityCode.VALIDATION_ERROR, "密码必须包含数字");
        Require.isTrue(!policyService.passwordRequireSpecialChar() || password.chars().anyMatch(this::isSpecialChar),
                IdentityCode.VALIDATION_ERROR, "密码必须包含特殊字符");
        String pattern = policyService.passwordPattern();
        if (StringUtils.hasText(pattern) && !matchesCustomPattern(pattern, password)) {
            Require.isTrue(false, IdentityCode.VALIDATION_ERROR, "密码不符合自定义规则");
        }
    }

    private boolean isSpecialChar(int codePoint) {
        return !Character.isLetterOrDigit(codePoint) && !Character.isWhitespace(codePoint);
    }

    private boolean matchesCustomPattern(String pattern, String password) {
        try {
            return Pattern.compile(pattern).matcher(password).matches();
        } catch (PatternSyntaxException ex) {
            Require.isTrue(false, IdentityCode.CONFIG_ERROR, "密码自定义正则配置错误");
            return false;
        }
    }
}
