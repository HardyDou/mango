package io.mango.ai.api.enums;

import lombok.Getter;

/** AI 厂商或协议类型。 */
@Getter
public enum AiProviderType {
    DEEPSEEK("DeepSeek", "deepseek", "https://api.deepseek.com", true),
    VOLCENGINE_ARK("火山方舟", "volcengine-ark", "https://ark.cn-beijing.volces.com/api/v3", true),
    ALIBABA_DASHSCOPE("阿里云百炼", "alibaba-dashscope",
            "https://dashscope.aliyuncs.com/compatible-mode/v1", true),
    ZHIPU("智谱 AI", "zhipu", "https://open.bigmodel.cn/api/paas/v4", true),
    SILICONFLOW("硅基流动", "siliconflow", "https://api.siliconflow.cn/v1", true),
    KIMI("Kimi", "kimi", "https://api.moonshot.cn/v1", true),
    OPENAI_COMPATIBLE("OpenAI 兼容协议", "openai-compatible", "https://api.openai.com", true),
    OLLAMA("Ollama", "ollama", "http://localhost:11434", false);

    private final String displayName;
    private final String defaultCode;
    private final String defaultBaseUrl;
    private final boolean apiKeyRequired;

    AiProviderType(String displayName, String defaultCode, String defaultBaseUrl, boolean apiKeyRequired) {
        this.displayName = displayName;
        this.defaultCode = defaultCode;
        this.defaultBaseUrl = defaultBaseUrl;
        this.apiKeyRequired = apiKeyRequired;
    }
}
