package io.mango.template.api.command;

import io.mango.template.api.enums.TemplateSourceFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 发布模板版本命令。
 */
@Data
@Schema(description = "发布模板版本命令")
public class PublishTemplateVersionCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Schema(description = "模板ID")
    private Long templateId;

    @NotNull
    @Schema(description = "内容稿源格式：TEXT、HTML、DOCX、XLSX")
    private TemplateSourceFormat sourceFormat;

    @Schema(description = "文本或HTML模板内容")
    private String content;

    @Schema(description = "DOCX或XLSX模板源文件ID")
    private Long sourceFileId;

    @Schema(description = "版本说明")
    private String versionRemark;

    @Schema(description = "模板变量定义，支持嵌套结构")
    private List<TemplateVariableDefinition> variables = new ArrayList<>();
}
