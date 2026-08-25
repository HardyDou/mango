package io.mango.ai.api.vo;

import io.mango.ai.api.enums.AiCapability;
import io.mango.ai.api.enums.AiServiceType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** AI 服务定义返回对象。 */
@Getter
@Setter
public class AiServiceVO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private AiServiceType serviceType;
    private AiCapability capability;
    private Long promptId;
    private String promptName;
    private Long skillId;
    private String skillName;
    private String inputSchemaJson;
    private String outputSchemaJson;
    private Boolean enabled;
    private LocalDateTime updatedAt;
}
