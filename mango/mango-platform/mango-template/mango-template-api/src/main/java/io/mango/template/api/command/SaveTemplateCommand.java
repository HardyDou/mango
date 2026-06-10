package io.mango.template.api.command;

import io.mango.template.api.enums.TemplateSourceFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 保存模板命令。
 */
@Data
@Schema(description = "保存模板命令")
public class SaveTemplateCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID，修改时必填")
    private Long id;

    @NotBlank
    @Schema(description = "模板编码")
    private String templateCode;

    @NotBlank
    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "分类编码")
    private String categoryCode;

    @Schema(description = "分类名称")
    private String categoryName;

    @NotBlank
    @Schema(description = "业务域编码")
    private String domainCode;

    @Deprecated
    @Schema(description = "业务组编码。兼容历史字段，前端不再使用")
    private String businessGroup;

    @Deprecated
    @Schema(description = "业务类型。兼容历史字段，前端不再使用")
    private String businessType;

    @Deprecated
    @Schema(description = "业务KEY。兼容历史字段，新调用统一使用模板编码")
    private String businessKey;

    @Schema(description = "当前模板源格式，首次创建可为空，发布内容稿时确定")
    private TemplateSourceFormat sourceFormat;

    @Schema(description = "未发布草稿文本或HTML内容")
    private String draftContent;

    @Schema(description = "未发布草稿DOCX或XLSX模板源文件ID")
    private Long draftSourceFileId;

    @Schema(description = "未发布草稿变量定义")
    private List<TemplateVariableDefinition> draftVariables = new ArrayList<>();

    @Schema(description = "备注")
    private String remark;
}
