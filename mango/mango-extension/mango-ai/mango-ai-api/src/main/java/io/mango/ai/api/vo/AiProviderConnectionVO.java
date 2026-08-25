package io.mango.ai.api.vo;

import io.mango.ai.api.enums.AiProviderType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** AI 厂商接入配置返回对象。 */
@Getter @Setter
public class AiProviderConnectionVO {
    private Long id;
    private String code;
    private String displayName;
    private AiProviderType providerType;
    private String baseUrl;
    private Boolean apiKeyConfigured;
    private String apiKeyHint;
    private Boolean enabled;
    private Integer modelCount;
    private LocalDateTime updatedAt;
}
