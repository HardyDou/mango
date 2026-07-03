package io.mango.notice.channel.sms;

import org.springframework.util.StringUtils;

import java.util.Map;

record AliyunSmsConfig(String accessKeyId, String accessKeySecret, String signName, String templateCode,
                       String endpoint) implements SmsProviderConfig {

    private static final String DEFAULT_ENDPOINT = "dysmsapi.aliyuncs.com";

    @Override
    public String providerCode() {
        return "ALIYUN";
    }

    static AliyunSmsConfig from(Map<String, Object> config, String commandTemplateCode) {
        String accessKeyId = secret(config, "accessKeyId", "accessKeyIdEnv", "阿里云 AccessKey ID");
        String accessKeySecret = secret(config, "accessKeySecret", "accessKeySecretEnv", "阿里云 AccessKey Secret");
        String signName = text(config, "signName");
        String templateCode = SmsNoticeChannelSender.firstText(commandTemplateCode, text(config, "templateCode"));
        if (!StringUtils.hasText(accessKeyId)) {
            throw new IllegalArgumentException("阿里云 AccessKey ID 不能为空");
        }
        if (!StringUtils.hasText(accessKeySecret)) {
            throw new IllegalArgumentException("阿里云 AccessKey Secret 不能为空");
        }
        if (!StringUtils.hasText(signName)) {
            throw new IllegalArgumentException("阿里云短信签名不能为空");
        }
        if (!StringUtils.hasText(templateCode)) {
            throw new IllegalArgumentException("阿里云短信模板 Code 不能为空");
        }
        String endpoint = text(config, "endpoint");
        return new AliyunSmsConfig(accessKeyId, accessKeySecret, signName, templateCode,
                StringUtils.hasText(endpoint) ? endpoint : DEFAULT_ENDPOINT);
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
