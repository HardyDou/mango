package io.mango.ai.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "供应商连接标识")
    private Long providerConnectionId;
    @NotBlank @Size(max = 128) @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._:/-]{0,127}$")
    @Schema(description = "供应商侧模型标识")
    private String modelName;
    @NotBlank @Size(max = 100)
    @Schema(description = "模型显示名称")
    private String displayName;
    @Size(max = 128)
    @Schema(description = "供应商平台别名")
    private String platformAlias;
    @NotNull
    @Schema(description = "模型调用协议")
    private AiApiProtocol apiProtocol;
    @NotEmpty
    @Schema(description = "模型能力集合")
    private Set<AiCapability> capabilities;
    @NotEmpty
    @Schema(description = "模型输入模态集合")
    private Set<AiModality> inputModalities;
    @NotEmpty
    @Schema(description = "模型输出模态集合")
    private Set<AiModality> outputModalities;
    @Size(max = 8192)
    @Schema(description = "模型扩展参数 JSON")
    private String parameterJson;
    @NotNull
    @Schema(description = "是否启用")
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
