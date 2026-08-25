package io.mango.ai.api.command;

import io.mango.ai.api.enums.AiProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 更新 AI 厂商接入配置。 */
@Getter
@Setter
public class UpdateAiProviderConnectionCommand {
    @NotNull @Positive
    private Long id;
    @NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")
    private String code;
    @NotBlank @Size(max = 100)
    private String displayName;
    @NotNull
    private AiProviderType providerType;
    @NotBlank @Size(max = 255)
    private String baseUrl;
    @Pattern(regexp = "^(?:|\\S{8,512})$")
    private String apiKey;
    @NotNull
    private Boolean enabled;
}
