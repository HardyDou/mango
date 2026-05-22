package io.mango.template.api.command;

import io.mango.template.api.enums.TemplateSourceFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 提取模板变量命令。
 */
@Data
@Schema(description = "提取模板变量命令")
public class ExtractTemplateVariablesCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Schema(description = "模板源格式：TEXT、HTML、DOCX、XLSX")
    private TemplateSourceFormat sourceFormat;

    @Schema(description = "文本或HTML模板内容")
    private String content;

    @Schema(description = "DOCX或XLSX模板源文件ID")
    private Long sourceFileId;
}
