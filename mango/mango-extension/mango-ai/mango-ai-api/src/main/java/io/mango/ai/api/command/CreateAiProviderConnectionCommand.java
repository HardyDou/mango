package io.mango.ai.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import io.mango.ai.api.enums.AiProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 创建 AI 厂商接入配置。 */
@Getter
@Setter
public class CreateAiProviderConnectionCommand {
    @NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")
    @Schema(description = "配置编码")
    private String code;
    @NotBlank @Size(max = 100)
    @Schema(description = "模型显示名称")
    private String displayName;
    @NotNull
    @Schema(description = "供应商类型")
    private AiProviderType providerType;
    @NotBlank @Size(max = 255)
    @Schema(description = "供应商 API 基础地址")
    private String baseUrl;
    @Size(max = 512) @Pattern(regexp = "^(?:|\\S{8,512})$")
    @Schema(description = "供应商 API 密钥；留空表示不修改或未配置")
    private String apiKey;
    @NotNull
    @Schema(description = "是否启用")
    private Boolean enabled;
}
