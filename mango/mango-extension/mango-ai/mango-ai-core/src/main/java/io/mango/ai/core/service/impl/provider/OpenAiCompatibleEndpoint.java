package io.mango.ai.core.service.impl.provider;

/** OpenAI 兼容协议的统一端点约定。 */
public final class OpenAiCompatibleEndpoint {

    private OpenAiCompatibleEndpoint() {
    }

    public static String apiBaseUrl(String baseUrl) {
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalized.endsWith("/v1") ? normalized.substring(0, normalized.length() - 3) : normalized;
    }

    public static String responsesUrl(String baseUrl) {
        return apiBaseUrl(baseUrl) + "/v1/responses";
    }
}
