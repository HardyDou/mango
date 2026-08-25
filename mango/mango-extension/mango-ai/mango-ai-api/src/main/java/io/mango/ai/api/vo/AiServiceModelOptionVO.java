package io.mango.ai.api.vo;

import io.mango.ai.api.enums.AiApiProtocol;
import io.mango.ai.api.enums.AiModality;
import lombok.Getter;
import lombok.Setter;

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
    private java.util.Set<AiModality> inputModalities;
    private java.util.Set<AiModality> outputModalities;
}
