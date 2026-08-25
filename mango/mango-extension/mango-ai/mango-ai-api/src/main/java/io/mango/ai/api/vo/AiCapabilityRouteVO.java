package io.mango.ai.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import io.mango.ai.api.enums.AiCapability;
import lombok.Getter;
import lombok.Setter;

/** AI 能力默认路由返回对象。 */
@Getter @Setter
public class AiCapabilityRouteVO {
    @Schema(description = "AI 能力类型")
    private AiCapability capability;
    @Schema(description = "模型标识")
    private Long modelId;
    @Schema(description = "模型显示名称")
    private String modelDisplayName;
    @Schema(description = "供应商显示名称")
    private String providerDisplayName;
}
