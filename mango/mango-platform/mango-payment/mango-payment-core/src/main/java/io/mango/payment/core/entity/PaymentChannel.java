package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_channel")
public class PaymentChannel extends AuditableEntity {

    private String channelCode;

    private String channelName;

    private String environment;

    private String channelType;

    private String adapterType;

    private String gatewayBaseUrl;

    private String fieldTemplateJson;

    private String capabilitySummary;

    private Integer status;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
