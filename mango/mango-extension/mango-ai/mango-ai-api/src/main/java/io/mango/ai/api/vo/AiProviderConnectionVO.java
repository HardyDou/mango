package io.mango.ai.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import io.mango.ai.api.enums.AiProviderType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** AI 厂商接入配置返回对象。 */
@Getter @Setter
public class AiProviderConnectionVO {
    @Schema(description = "记录标识")
    private Long id;
    @Schema(description = "配置编码")
    private String code;
    @Schema(description = "显示名称")
    private String displayName;
    @Schema(description = "供应商类型")
    private AiProviderType providerType;
    @Schema(description = "供应商 API 基础地址")
    private String baseUrl;
    @Schema(description = "是否已配置 API 密钥")
    private Boolean apiKeyConfigured;
    @Schema(description = "API 密钥末尾提示")
    private String apiKeyHint;
    @Schema(description = "是否启用")
    private Boolean enabled;
    @Schema(description = "供应商模型数量")
    private Integer modelCount;
    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;
}
