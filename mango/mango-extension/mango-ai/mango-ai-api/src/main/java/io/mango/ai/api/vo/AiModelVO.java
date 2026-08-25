package io.mango.ai.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "记录标识")
    private Long id;
    @Schema(description = "供应商连接标识")
    private Long providerConnectionId;
    @Schema(description = "供应商侧模型标识")
    private String modelName;
    @Schema(description = "显示名称")
    private String displayName;
    @Schema(description = "供应商平台别名")
    private String platformAlias;
    @Schema(description = "模型调用协议")
    private AiApiProtocol apiProtocol;
    @Schema(description = "模型能力集合")
    private Set<AiCapability> capabilities;
    @Schema(description = "模型输入模态集合")
    private Set<AiModality> inputModalities;
    @Schema(description = "模型输出模态集合")
    private Set<AiModality> outputModalities;
    @Schema(description = "模型扩展参数 JSON")
    private String parameterJson;
    @Schema(description = "是否启用")
    private Boolean enabled;
    @Schema(description = "当前是否可调用")
    private Boolean callable;
    @Schema(description = "已绑定的默认能力路由集合")
    private Set<AiCapability> routedCapabilities;
    @Schema(description = "最后更新时间")
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
