package io.mango.ai.api.command;

import io.mango.ai.api.enums.AiCapability;
import io.mango.ai.api.enums.AiServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 创建 AI 服务定义。 */
@Getter
@Setter
public class CreateAiServiceCommand {
    @NotBlank
    @Size(max = 64)
    private String code;
    @NotBlank
    @Size(max = 100)
    private String name;
    @Size(max = 500)
    private String description;
    @NotNull
    private AiServiceType serviceType;
    private AiCapability capability;
    private Long promptId;
    private Long skillId;
    @NotBlank
    @Size(max = 65535)
    private String inputSchemaJson;
    @NotBlank
    @Size(max = 65535)
    private String outputSchemaJson;
    @NotNull
    private Boolean enabled;
}
