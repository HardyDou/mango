package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_channel_bill_source")
public class PaymentChannelBillSourceEntity extends PaymentBaseEntity {

    private Long contractId;

    private String channelCode;

    private String fetchMode;

    private String endpoint;

    private String remotePath;

    private String credentialRef;

    private String pageMode;

    private Integer enabled;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
