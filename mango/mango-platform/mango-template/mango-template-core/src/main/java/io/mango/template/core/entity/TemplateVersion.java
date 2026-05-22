package io.mango.template.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模板版本实体。
 */
@Data
@TableName("template_version")
public class TemplateVersion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long templateId;
    private Integer versionNo;
    private String sourceFormat;
    private String content;
    private Long sourceFileId;
    private String variableSchema;
    private Integer currentPublished;
    private String versionRemark;
    private Long createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private LocalDateTime updatedAt;
}
