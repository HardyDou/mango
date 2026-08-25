package io.mango.ai.api.vo;

import io.mango.ai.api.enums.AiCapability;
import lombok.Getter;
import lombok.Setter;

/** AI 能力默认路由返回对象。 */
@Getter @Setter
public class AiCapabilityRouteVO {
    private AiCapability capability;
    private Long modelId;
    private String modelDisplayName;
    private String providerDisplayName;
}
