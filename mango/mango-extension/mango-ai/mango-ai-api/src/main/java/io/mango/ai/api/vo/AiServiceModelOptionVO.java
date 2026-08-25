package io.mango.ai.api.vo;

import io.mango.ai.api.enums.AiApiProtocol;
import io.mango.ai.api.enums.AiModality;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/** AI 服务单次运行可选择的模型。 */
@Getter
@Setter
public class AiServiceModelOptionVO {
    private Long modelId;
    private String modelName;
    private String displayName;
    private String providerCode;
    private String providerDisplayName;
    private AiApiProtocol apiProtocol;
    private Boolean thinkingConfigurable;
    private Set<AiModality> inputModalities;
    private Set<AiModality> outputModalities;

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
