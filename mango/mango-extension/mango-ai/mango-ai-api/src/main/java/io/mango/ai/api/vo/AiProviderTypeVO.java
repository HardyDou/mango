package io.mango.ai.api.vo;

import io.mango.ai.api.enums.AiProviderType;
import lombok.Getter;
import lombok.Setter;

/** 厂商类型选项。 */
@Getter @Setter
public class AiProviderTypeVO {
    private AiProviderType code;
    private String name;
    private String defaultCode;
    private String defaultBaseUrl;
    private Boolean apiKeyRequired;
}
