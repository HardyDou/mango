package io.mango.ai.api.vo;

import io.mango.ai.api.enums.AiPromptStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Prompt 模板返回对象。 */
@Getter
@Setter
public class AiPromptVO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String template;
    private String variablesJson;
    private AiPromptStatus status;
    private Integer version;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
}
