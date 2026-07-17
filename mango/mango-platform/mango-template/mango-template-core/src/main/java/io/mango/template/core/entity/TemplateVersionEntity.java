package io.mango.template.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模板版本实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("template_version")
public class TemplateVersionEntity extends TenantEntity {

    private Long templateId;
    private Integer versionNo;
    private String sourceFormat;
    private String content;
    private Long sourceFileId;
    private String variableSchema;
    private Integer currentPublished;
    private String versionRemark;
}
