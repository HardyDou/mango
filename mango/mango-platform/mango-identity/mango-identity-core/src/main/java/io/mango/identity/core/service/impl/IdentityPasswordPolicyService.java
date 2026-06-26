package io.mango.identity.core.service.impl;

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
public class IdentityPasswordPolicyService {

    private final IdentitySecurityPolicyService policyService;

    /**
     * 校验明文密码是否满足安全基线。
     */
    public void validatePlainPassword(String password) {
        if (!policyService.passwordComplexityEnabled()) {
            return;
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (password.length() < policyService.passwordMinLength()) {
            throw new IllegalArgumentException("密码长度至少" + policyService.passwordMinLength() + "位");
        }
        if (!policyService.passwordAllowWhitespace() && password.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("密码不能包含空白字符");
        }
        if (policyService.passwordRequireLetter() && password.chars().noneMatch(Character::isLetter)) {
            throw new IllegalArgumentException("密码必须包含字母");
        }
        if (policyService.passwordRequireDigit() && password.chars().noneMatch(Character::isDigit)) {
            throw new IllegalArgumentException("密码必须包含数字");
        }
        if (policyService.passwordRequireSpecialChar() && password.chars().noneMatch(this::isSpecialChar)) {
            throw new IllegalArgumentException("密码必须包含特殊字符");
        }
        String pattern = policyService.passwordPattern();
        if (StringUtils.hasText(pattern) && !matchesCustomPattern(pattern, password)) {
            throw new IllegalArgumentException("密码不符合自定义规则");
        }
    }

    private boolean isSpecialChar(int codePoint) {
        return !Character.isLetterOrDigit(codePoint) && !Character.isWhitespace(codePoint);
    }

    private boolean matchesCustomPattern(String pattern, String password) {
        try {
            return Pattern.compile(pattern).matcher(password).matches();
        } catch (PatternSyntaxException ex) {
            throw new IllegalArgumentException("密码自定义正则配置错误");
        }
    }
}
