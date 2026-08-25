package io.mango.ai.api.command;

import io.mango.ai.api.enums.AiCapability;
import io.mango.ai.api.enums.AiApiProtocol;
import io.mango.ai.api.enums.AiModality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/** 创建供应商模型目录项。 */
@Getter
@Setter
public class CreateAiModelCommand {
    @NotNull
    private Long providerConnectionId;
    @NotBlank @Size(max = 128) @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._:/-]{0,127}$")
    private String modelName;
    @NotBlank @Size(max = 100)
    private String displayName;
    @Size(max = 128)
    private String platformAlias;
    @NotNull
    private AiApiProtocol apiProtocol;
    @NotEmpty
    private Set<AiCapability> capabilities;
    @NotEmpty
    private Set<AiModality> inputModalities;
    @NotEmpty
    private Set<AiModality> outputModalities;
    @Size(max = 8192)
    private String parameterJson;
    @NotNull
    private Boolean enabled;
}
