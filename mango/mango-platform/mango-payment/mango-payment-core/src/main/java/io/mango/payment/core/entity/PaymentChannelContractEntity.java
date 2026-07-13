package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_channel_contract")
public class PaymentChannelContractEntity extends PaymentBaseEntity {

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

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
