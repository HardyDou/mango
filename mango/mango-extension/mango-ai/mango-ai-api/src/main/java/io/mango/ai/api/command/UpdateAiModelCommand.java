package io.mango.ai.api.command;

import io.mango.ai.api.enums.AiCapability;
import io.mango.ai.api.enums.AiApiProtocol;
import io.mango.ai.api.enums.AiModality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/** 更新供应商模型目录项。 */
@Getter
@Setter
public class UpdateAiModelCommand {
    @NotNull @Positive
    private Long id;
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

    public Set<AiCapability> getCapabilities() {
        return capabilities == null ? null : Set.copyOf(capabilities);
    }

    public void setCapabilities(Set<AiCapability> capabilities) {
        this.capabilities = capabilities == null ? null : Set.copyOf(capabilities);
    }

    public Set<AiModality> getInputModalities() {
        return inputModalities == null ? null : Set.copyOf(inputModalities);
    }

    public void setInputModalities(Set<AiModality> inputModalities) {
        this.inputModalities = inputModalities == null ? null : Set.copyOf(inputModalities);
    }

    public Set<AiModality> getOutputModalities() {
        return outputModalities == null ? null : Set.copyOf(outputModalities);
    }

    public void setOutputModalities(Set<AiModality> outputModalities) {
        this.outputModalities = outputModalities == null ? null : Set.copyOf(outputModalities);
    }
}
