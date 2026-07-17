package io.mango.ai.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DeepSeek 默认实现配置。
 *
 * @param baseUrl API 基础地址
 * @param apiKey API 密钥
 * @param model 模型名称
 * @param connectTimeout 连接超时，毫秒
 * @param readTimeout 读取超时，毫秒
 */
@ConfigurationProperties("mango.ai.deepseek")
public record MangoAiProperties(
        String baseUrl,
        String apiKey,
        String model,
        Integer connectTimeout,
        Integer readTimeout) {

    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final int DEFAULT_CONNECT_TIMEOUT = 10_000;
    private static final int DEFAULT_READ_TIMEOUT = 60_000;

    public MangoAiProperties {
        baseUrl = valueOrDefault(baseUrl, DEFAULT_BASE_URL);
        if (apiKey == null) {
            apiKey = "";
        }
        model = valueOrDefault(model, DEFAULT_MODEL);
        connectTimeout = positiveOrDefault(connectTimeout, DEFAULT_CONNECT_TIMEOUT);
        readTimeout = positiveOrDefault(readTimeout, DEFAULT_READ_TIMEOUT);
    }

    private static String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static int positiveOrDefault(Integer value, int defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
    }
}
