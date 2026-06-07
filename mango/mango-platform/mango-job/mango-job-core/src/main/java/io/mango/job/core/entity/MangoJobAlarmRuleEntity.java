package io.mango.job.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * Job 告警规则实体。
 */
@Getter
@Setter
@TableName("mango_job_alarm_rule")
public class MangoJobAlarmRuleEntity extends TenantEntity {

    private Long jobId;

    private String appCode;

    private String ruleName;

    private String alarmType;

    private String triggerCondition;

    private String noticeSceneCode;

    private String noticeTemplateCode;

    private String noticeParams;

    private Integer enabled;
}
