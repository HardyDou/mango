package io.mango.template.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 启用模板版本命令。
 */
@Data
@Schema(description = "启用模板版本命令")
public class ActivateTemplateVersionCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Schema(description = "模板ID")
    private Long templateId;

    @NotNull
    @Schema(description = "待启用版本号")
    private Integer versionNo;
}
