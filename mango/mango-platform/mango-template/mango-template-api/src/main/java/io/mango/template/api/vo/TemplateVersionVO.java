package io.mango.template.api.vo;

import io.mango.template.api.command.TemplateVariableDefinition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 模板版本视图。
 */
@Data
@Schema(description = "模板版本视图")
public class TemplateVersionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板版本ID")
    private Long id;
    @Schema(description = "模板ID")
    private Long templateId;
    @Schema(description = "版本号")
    private Integer versionNo;
    @Schema(description = "内容稿源格式")
    private String sourceFormat;
    @Schema(description = "文本或HTML模板内容")
    private String content;
    @Schema(description = "DOCX或XLSX模板源文件ID")
    private Long sourceFileId;
    @Schema(description = "变量定义JSON")
    private String variableSchema;
    @Schema(description = "结构化变量定义")
    private List<TemplateVariableDefinition> variables = new ArrayList<>();
    @Schema(description = "是否当前发布版本：0否，1是")
    private Integer currentPublished;
    @Schema(description = "版本说明")
    private String versionRemark;
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
}
