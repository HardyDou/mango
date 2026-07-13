package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_channel_contract_capability")
public class PaymentChannelContractCapabilityEntity extends PaymentBaseEntity {

    private Long contractId;

    private Long channelCapabilityId;

    private String methodCode;

    private String terminalType;

    private BigDecimal feeRate;

    private Long minAmount;

    private Long maxAmount;

    private Integer priority;

    private LocalDateTime certificateExpireTime;

    private Integer status;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
