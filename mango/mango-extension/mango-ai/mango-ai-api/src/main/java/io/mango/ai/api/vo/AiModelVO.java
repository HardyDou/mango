package io.mango.ai.api.vo;

import io.mango.ai.api.enums.AiCapability;
import io.mango.ai.api.enums.AiApiProtocol;
import io.mango.ai.api.enums.AiModality;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

/** 供应商模型目录返回对象。 */
@Getter @Setter
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
}
