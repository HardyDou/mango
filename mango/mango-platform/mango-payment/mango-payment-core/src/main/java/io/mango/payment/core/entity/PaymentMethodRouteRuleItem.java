package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_method_route_rule_item")
public class PaymentMethodRouteRuleItem extends AuditableEntity {

    private Long ruleId;

    private Long contractCapabilityId;

    private Integer priority;

    private Integer weight;

    private Long minAmount;

    private Long maxAmount;

    private Integer status;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
