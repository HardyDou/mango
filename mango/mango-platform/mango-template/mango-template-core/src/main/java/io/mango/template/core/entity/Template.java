package io.mango.template.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模板主表实体。
 */
@Data
@TableName("template")
public class Template {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String templateCode;
    private String templateName;
    private String categoryCode;
    private String categoryName;
    private String domainCode;
    private String businessGroup;
    private String businessType;
    private String businessKey;
    private String sourceFormat;
    private Integer status;
    private Integer currentVersionNo;
    private String draftSourceFormat;
    private String draftContent;
    private Long draftSourceFileId;
    private String draftVariableSchema;
    private Integer hasUnpublishedChanges;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private LocalDateTime updatedAt;
}
