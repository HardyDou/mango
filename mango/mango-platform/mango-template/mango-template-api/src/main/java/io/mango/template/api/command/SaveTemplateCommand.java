package io.mango.template.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 创建和修改模板共享的保存字段。
 */
@Data
@Schema(description = "模板保存字段")
public class SaveTemplateCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "模板编码不能为空")
    @Size(max = 128, message = "模板编码不能超过128个字符")
    @Schema(description = "模板编码")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128, message = "模板名称不能超过128个字符")
    @Schema(description = "模板名称")
    private String templateName;

    @Size(max = 64, message = "分类编码不能超过64个字符")
    @Schema(description = "分类编码")
    private String categoryCode;

    @Size(max = 128, message = "分类名称不能超过128个字符")
    @Schema(description = "分类名称")
    private String categoryName;

    @NotBlank(message = "业务域编码不能为空")
    @Size(max = 64, message = "业务域编码不能超过64个字符")
    @Schema(description = "业务域编码")
    private String domainCode;

    @Deprecated
    @Size(max = 64, message = "业务组编码不能超过64个字符")
    @Schema(description = "业务组编码。兼容历史字段，前端不再使用")
    private String businessGroup;

    @Deprecated
    @Size(max = 64, message = "业务类型不能超过64个字符")
    @Schema(description = "业务类型。兼容历史字段，前端不再使用")
    private String businessType;

    @Deprecated
    @Size(max = 128, message = "业务KEY不能超过128个字符")
    @Schema(description = "业务KEY。兼容历史字段，新调用统一使用模板编码")
    private String businessKey;

    @Size(max = 32, message = "模板源格式不能超过32个字符")
    @Schema(description = "当前模板源格式，首次创建可为空，发布内容稿时确定")
    private String sourceFormat;

    @Size(max = 10485760, message = "草稿内容不能超过10MB")
    @Schema(description = "未发布草稿文本或HTML内容")
    private String draftContent;

    @Positive(message = "草稿源文件ID必须为正数")
    @Schema(description = "未发布草稿DOCX或XLSX模板源文件ID")
    private Long draftSourceFileId;

    @Valid
    @Size(max = 200, message = "草稿变量不能超过200项")
    @Schema(description = "未发布草稿变量定义")
    private List<TemplateVariableCommand> draftVariables = new ArrayList<>();

    @Size(max = 255, message = "备注不能超过255个字符")
    @Schema(description = "备注")
    private String remark;
}
