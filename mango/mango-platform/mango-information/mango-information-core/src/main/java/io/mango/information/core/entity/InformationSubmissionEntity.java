package io.mango.information.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 信息采集提交实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("information_submission")
public class InformationSubmissionEntity extends TenantEntity {
    private Long formId;
    private String formCode;
    private String valuesJson;
    private String sourcePage;
    private String idempotencyKey;
    private String status;
    private String processRemark;
    private LocalDateTime submittedAt;
}
