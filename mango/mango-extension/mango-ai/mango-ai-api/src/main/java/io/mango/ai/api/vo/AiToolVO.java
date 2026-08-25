package io.mango.ai.api.vo;

import io.mango.ai.api.enums.AiToolType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** AI 工具返回对象。 */
@Getter
@Setter
public class AiToolVO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private AiToolType toolType;
    private String endpoint;
    private String inputSchemaJson;
    private String outputSchemaJson;
    private Boolean enabled;
    private LocalDateTime updatedAt;
}
