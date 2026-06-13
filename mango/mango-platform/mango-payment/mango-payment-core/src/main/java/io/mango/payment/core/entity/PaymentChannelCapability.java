package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_channel_capability")
public class PaymentChannelCapability extends AuditableEntity {

    private Long channelId;

    private String methodCode;

    private String terminalType;

    private String environment;

    private Integer supportsRefund;

    private Integer supportsQuery;

    private Integer supportsClose;

    private Integer supportsBill;

    private Integer supportsReconcile;

    private Long minAmount;

    private Long maxAmount;

    private Integer status;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
