package io.mango.ai.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import io.mango.ai.api.enums.AiProviderType;
import lombok.Getter;
import lombok.Setter;

/** 厂商类型选项。 */
@Getter @Setter
public class AiProviderTypeVO {
    @Schema(description = "配置编码")
    private AiProviderType code;
    @Schema(description = "显示名称")
    private String name;
    @Schema(description = "供应商默认编码")
    private String defaultCode;
    @Schema(description = "供应商默认 API 地址")
    private String defaultBaseUrl;
    @Schema(description = "是否必须配置 API 密钥")
    private Boolean apiKeyRequired;
}
