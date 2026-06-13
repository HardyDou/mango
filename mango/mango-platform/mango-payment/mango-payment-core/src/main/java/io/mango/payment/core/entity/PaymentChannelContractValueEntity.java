package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_channel_contract_value")
public class PaymentChannelContractValueEntity extends AuditableEntity {

    private Long contractId;

    private String fieldCode;

    private String valueText;

    private String encryptedValue;

    private Long fileId;

    private String valueSource;

    private Integer sensitiveFlag;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
