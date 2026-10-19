package io.mango.information.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 提交时字段定义快照。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("information_submission_snapshot")
public class InformationSubmissionSnapshotEntity extends TenantEntity {
    private Long submissionId;
    private Long formId;
    private String definitionJson;
}
