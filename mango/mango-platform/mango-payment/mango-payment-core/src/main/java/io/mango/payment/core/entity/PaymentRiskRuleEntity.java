package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_risk_rule")
public class PaymentRiskRuleEntity extends AuditableEntity {

    private String ruleCode;

    private String ruleName;

    private String ruleScope;

    private Long appId;

    private Long subjectId;

    private String methodCode;

    private String riskType;

    private Long thresholdAmount;

    private String periodType;

    private Integer periodLimitCount;

    private Long periodLimitAmount;

    private String actionType;

    private Integer priority;

    private Integer status;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
