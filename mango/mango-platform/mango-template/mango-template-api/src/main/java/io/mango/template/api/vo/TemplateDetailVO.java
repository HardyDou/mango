package io.mango.template.api.vo;

import io.mango.template.api.command.TemplateVariableDefinition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 模板详情。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "模板详情")
public class TemplateDetailVO extends TemplateVO {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板版本列表")
    private List<TemplateVersionVO> versions = new ArrayList<>();

    @Schema(description = "未发布草稿文本或HTML内容")
    private String draftContent;

    @Schema(description = "未发布草稿DOCX或XLSX模板源文件ID")
    private Long draftSourceFileId;

    @Schema(description = "结构化未发布草稿变量定义")
    private List<TemplateVariableDefinition> draftVariables = new ArrayList<>();
}
