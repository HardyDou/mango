package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_channel_contract")
public class PaymentChannelContract extends AuditableEntity {

    private String contractCode;

    private String contractName;

    private Long subjectId;

    private Long channelId;

    private String environment;

    private String merchantNo;

    private String appId;

    private String configValuesJson;

    private String enabledMethodCodes;

    private Integer status;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
