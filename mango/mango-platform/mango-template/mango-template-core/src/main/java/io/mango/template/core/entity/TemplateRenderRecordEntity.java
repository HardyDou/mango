package io.mango.template.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模板渲染记录实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("template_render_record")
public class TemplateRenderRecordEntity extends TenantEntity {

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
}
