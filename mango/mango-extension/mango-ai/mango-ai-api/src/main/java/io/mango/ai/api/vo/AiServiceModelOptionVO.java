package io.mango.ai.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import io.mango.ai.api.enums.AiApiProtocol;
import io.mango.ai.api.enums.AiModality;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/** AI 服务单次运行可选择的模型。 */
@Getter
@Setter
public class AiServiceModelOptionVO {
    @Schema(description = "模型标识")
    private Long modelId;
    @Schema(description = "供应商侧模型标识")
    private String modelName;
    @Schema(description = "显示名称")
    private String displayName;
    @Schema(description = "供应商编码")
    private String providerCode;
    @Schema(description = "供应商显示名称")
    private String providerDisplayName;
    @Schema(description = "模型调用协议")
    private AiApiProtocol apiProtocol;
    @Schema(description = "是否支持配置思考模式")
    private Boolean thinkingConfigurable;
    @Schema(description = "模型输入模态集合")
    private Set<AiModality> inputModalities;
    @Schema(description = "模型输出模态集合")
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
