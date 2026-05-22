package io.mango.template.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模板渲染记录实体。
 */
@Data
@TableName("template_render_record")
public class TemplateRenderRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long templateId;
    private String templateCode;
    private Long versionId;
    private Integer versionNo;
    private String outputFormat;
    private String status;
    private Long outputFileId;
    private String outputContent;
    private String errorMessage;
    private String variablePayload;
    private String bizType;
    private String bizId;
    private Long createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private LocalDateTime updatedAt;
}
