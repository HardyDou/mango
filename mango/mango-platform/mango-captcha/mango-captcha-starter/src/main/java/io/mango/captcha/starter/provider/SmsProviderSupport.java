package io.mango.captcha.starter.provider;

import io.mango.captcha.api.enums.CaptchaCode;
import io.mango.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 内置短信供应商的公共校验与错误构造工具。
 *
 * <p>所有校验失败统一抛出 {@link CaptchaCode#CAPTCHA_SEND_FAILED}，详细原因只写入日志，
 * 日志中不包含手机号明文、凭证值等敏感信息。</p>
 *
 * @author Mango
 */
@Slf4j
final class SmsProviderSupport {

    private static final Pattern COUNTRY_CODE_PATTERN = Pattern.compile("\\+\\d{1,4}");

    private SmsProviderSupport() {
    }

    /**
     * 构造统一的发送失败业务异常。
     *
     * @return 发送失败业务异常
     */
    static BizException sendFailed() {
        return new BizException(CaptchaCode.CAPTCHA_SEND_FAILED.getCode(),
                CaptchaCode.CAPTCHA_SEND_FAILED.getMessage());
    }

    /**
     * 校验调用方传入的必填参数（手机号、模板编码等）。
     *
     * @param providerName 供应商名称
     * @param field        字段名（只记录字段名，不记录取值）
     * @param value        字段取值
     */
    static void requireText(String providerName, String field, String value) {
        if (!StringUtils.hasText(value)) {
            log.error("验证码短信参数缺失: provider={}, field={}", providerName, field);
            throw sendFailed();
        }
    }

    /**
     * 校验配置项是否已配置（如凭证、签名、国家码）。
     *
     * @param providerName 供应商名称
     * @param property     配置项键名
     * @param value        配置值
     */
    static void requireConfigured(String providerName, String property, String value) {
        if (!StringUtils.hasText(value)) {
            log.error("验证码短信配置缺失: provider={}, property={}", providerName, property);
            throw sendFailed();
        }
    }

    /**
     * 规范化国家码为 E.164 前缀（如 {@code 86} 补齐为 {@code +86}）。
     *
     * @param providerName 供应商名称
     * @param countryCode  配置的国家码
     * @return 以 {@code +} 开头的国家码
     */
    static String normalizeCountryCode(String providerName, String countryCode) {
        String normalized = countryCode == null ? null : countryCode.trim();
        if (StringUtils.hasText(normalized) && !normalized.startsWith("+")) {
            normalized = "+" + normalized;
        }
        if (!StringUtils.hasText(normalized) || !COUNTRY_CODE_PATTERN.matcher(normalized).matches()) {
            log.error("验证码短信国家码非法: provider={}, countryCode={}", providerName, countryCode);
            throw sendFailed();
        }
        return normalized;
    }

    /**
     * 取短信模板的第一个参数（验证码）。
     *
     * @param providerName 供应商名称
     * @param params       模板参数
     * @return 验证码
     */
    static String requireTemplateParam(String providerName, String... params) {
        String value = params == null || params.length == 0 ? null : params[0];
        if (!StringUtils.hasText(value)) {
            log.error("验证码短信参数缺失: provider={}, field=params[0]", providerName);
            throw sendFailed();
        }
        return value;
    }

    /**
     * 配置项为空时回退默认值。
     *
     * @param value        配置值
     * @param defaultValue 默认值
     * @return 实际使用的值
     */
    static String configOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
