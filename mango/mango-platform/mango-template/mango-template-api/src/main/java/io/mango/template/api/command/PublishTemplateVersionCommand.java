package io.mango.template.api.command;

import io.mango.template.api.enums.TemplateSourceFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
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

    @Size(max = 10485760, message = "模板内容不能超过10MB")
    @Schema(description = "文本或HTML模板内容")
    private String content;

    @Positive(message = "模板源文件ID必须为正数")
    @Schema(description = "DOCX或XLSX模板源文件ID")
    private Long sourceFileId;

    @Size(max = 255, message = "版本说明不能超过255个字符")
    @Schema(description = "版本说明")
    private String versionRemark;

    @Valid
    @Size(max = 200, message = "模板变量不能超过200项")
    @Schema(description = "模板变量定义，支持嵌套结构")
    private List<TemplateVariableCommand> variables = new ArrayList<>();

    public List<TemplateVariableCommand> getVariables() {
        if (variables == null) {
            return null;
        }
        return new ArrayList<>(variables);
    }

    public void setVariables(List<TemplateVariableCommand> variables) {
        if (variables == null) {
            this.variables = null;
            return;
        }
        this.variables = new ArrayList<>(variables);
    }
}
