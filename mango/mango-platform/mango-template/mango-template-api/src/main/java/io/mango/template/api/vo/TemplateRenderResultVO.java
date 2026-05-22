package io.mango.template.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 模板渲染结果。
 */
@Data
@Schema(description = "模板渲染结果")
public class TemplateRenderResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "渲染记录ID")
    private Long recordId;
    @Schema(description = "渲染状态")
    private String status;
    @Schema(description = "文本类渲染内容")
    private String content;
    @Schema(description = "文档类渲染产物文件ID")
    private Long fileId;
    @Schema(description = "文档类渲染产物文件名")
    private String fileName;
    @Schema(description = "文档类渲染产物内容类型")
    private String contentType;
    @Schema(description = "失败原因")
    private String errorMessage;
}
