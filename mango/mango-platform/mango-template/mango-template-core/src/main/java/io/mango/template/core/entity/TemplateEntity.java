package io.mango.template.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模板主表实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("template")
public class TemplateEntity extends TenantEntity {

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
}
