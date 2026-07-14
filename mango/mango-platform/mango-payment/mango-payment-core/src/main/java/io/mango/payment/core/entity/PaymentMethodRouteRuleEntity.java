package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_method_route_rule")
public class PaymentMethodRouteRuleEntity extends PaymentBaseEntity {

    private String ruleCode;

    private String ruleName;

    private Long appId;

    private Long subjectId;

    private String methodCode;

    private String terminalType;

    private String environment;

    private String routeMode;

    private Integer fallbackEnabled;

    private Integer status;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
