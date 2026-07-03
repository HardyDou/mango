package io.mango.notice.channel.sms;

import org.springframework.util.StringUtils;

import java.util.Map;

record TencentSmsConfig(String secretId, String secretKey, String smsSdkAppId, String signName, String templateCode,
                        String region, String endpoint, String countryCode) implements SmsProviderConfig {

    private static final String DEFAULT_REGION = "ap-guangzhou";
    private static final String DEFAULT_ENDPOINT = "sms.tencentcloudapi.com";
    private static final String DEFAULT_COUNTRY_CODE = "+86";

    @Override
    public String providerCode() {
        return "TENCENT";
    }

    static TencentSmsConfig from(Map<String, Object> config, String commandTemplateCode) {
        String secretId = secret(config, "secretId", "secretIdEnv", "腾讯云 SecretId");
        String secretKey = secret(config, "secretKey", "secretKeyEnv", "腾讯云 SecretKey");
        String smsSdkAppId = SmsNoticeChannelSender.firstText(text(config, "smsSdkAppId"), text(config, "appId"));
        String signName = text(config, "signName");
        String templateCode = SmsNoticeChannelSender.firstText(commandTemplateCode, text(config, "templateCode"),
                text(config, "templateId"));
        if (!StringUtils.hasText(secretId)) {
            throw new IllegalArgumentException("腾讯云 SecretId 不能为空");
        }
        if (!StringUtils.hasText(secretKey)) {
            throw new IllegalArgumentException("腾讯云 SecretKey 不能为空");
        }
        if (!StringUtils.hasText(smsSdkAppId)) {
            throw new IllegalArgumentException("腾讯云 SmsSdkAppId 不能为空");
        }
        if (!StringUtils.hasText(signName)) {
            throw new IllegalArgumentException("腾讯云短信签名不能为空");
        }
        if (!StringUtils.hasText(templateCode)) {
            throw new IllegalArgumentException("腾讯云短信模板 Code 不能为空");
        }
        String region = text(config, "region");
        String endpoint = text(config, "endpoint");
        String countryCode = text(config, "countryCode");
        return new TencentSmsConfig(secretId, secretKey, smsSdkAppId, signName, templateCode,
                StringUtils.hasText(region) ? region : DEFAULT_REGION,
                StringUtils.hasText(endpoint) ? endpoint : DEFAULT_ENDPOINT,
                StringUtils.hasText(countryCode) ? countryCode : DEFAULT_COUNTRY_CODE);
    }

    private static String secret(Map<String, Object> config, String valueKey, String envKey, String label) {
        String value = text(config, valueKey);
        if (StringUtils.hasText(value)) {
            return value;
        }
        String envName = text(config, envKey);
        if (!StringUtils.hasText(envName)) {
            return null;
        }
        String envValue = System.getenv(envName);
        if (!StringUtils.hasText(envValue)) {
            throw new IllegalArgumentException(label + " 环境变量未配置：" + envName);
        }
        return envValue;
    }

    private static String text(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }
}
