package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_cashier_config")
public class PaymentCashierConfig extends AuditableEntity {

    private String cashierName;

    private Long applicationId;

    private Integer defaultCashier;

    private String enterpriseSubjectIds;

    private String methodCodes;

    private String defaultMethodCode;

    private String methodDisplayOrder;

    private String resultReturnUrl;

    private String displayConfig;

    private Integer status;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
