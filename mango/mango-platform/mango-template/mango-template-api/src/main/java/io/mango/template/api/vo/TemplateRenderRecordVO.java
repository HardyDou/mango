package io.mango.template.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模板渲染记录。
 */
@Data
@Schema(description = "模板渲染记录")
public class TemplateRenderRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "渲染记录ID")
    private Long id;
    @Schema(description = "租户ID")
    private Long tenantId;
    @Schema(description = "模板ID")
    private Long templateId;
    @Schema(description = "模板编码")
    private String templateCode;
    @Schema(description = "模板版本ID")
    private Long versionId;
    @Schema(description = "模板版本号")
    private Integer versionNo;
    @Schema(description = "输出格式")
    private String outputFormat;
    @Schema(description = "渲染状态")
    private String status;
    @Schema(description = "输出文件ID")
    private Long outputFileId;
    @Schema(description = "文本类输出内容")
    private String outputContent;
    @Schema(description = "失败原因")
    private String errorMessage;
    @Schema(description = "业务类型")
    private String bizType;
    @Schema(description = "业务ID")
    private String bizId;
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
