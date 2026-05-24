package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("mango_pay_tenant_cashier")
public class PaymentTenantCashier {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private Long tenantId;

    private String tenantName;

    private String appCode;

    private String cashierCode;

    private String cashierName;

    private String enabledMethods;

    private String defaultMethod;

    private Integer expireMinutes;

    private Long dailyLimit;
}
