package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_tenant")
public class PaymentTenantEntity extends PaymentBaseEntity {

    private String tenantCode;

    private String tenantName;

    private Long platformTenantId;

    private Integer status;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
