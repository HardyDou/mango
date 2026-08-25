package io.mango.ai.api.vo;

import io.mango.ai.api.enums.AiCapability;
import io.mango.ai.api.enums.AiApiProtocol;
import io.mango.ai.api.enums.AiModality;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

/** 供应商模型目录返回对象。 */
@Getter
@Setter
public class AiModelVO {
    private Long id;
    private Long providerConnectionId;
    private String modelName;
    private String displayName;
    private String platformAlias;
    private AiApiProtocol apiProtocol;
    private Set<AiCapability> capabilities;
    private Set<AiModality> inputModalities;
    private Set<AiModality> outputModalities;
    private String parameterJson;
    private Boolean enabled;
    private Boolean callable;
    private Set<AiCapability> routedCapabilities;
    private LocalDateTime updatedAt;

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

    public Set<AiCapability> getRoutedCapabilities() {
        return routedCapabilities == null ? null : Set.copyOf(routedCapabilities);
    }

    public void setRoutedCapabilities(Set<AiCapability> routedCapabilities) {
        this.routedCapabilities = routedCapabilities == null ? null : Set.copyOf(routedCapabilities);
    }
}
