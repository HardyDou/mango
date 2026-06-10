package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_channel_bill_detail")
public class PaymentChannelBillDetailEntity extends AuditableEntity {

    private Long reconciliationId;

    private String batchNo;

    private String channelCode;

    private LocalDate billDate;

    private String channelTradeNo;

    private String tradeType;

    private Long amount;

    private Long fee;

    private LocalDateTime tradeTime;

    private String matchStatus;

    private String matchedOrderNo;

    private String matchMessage;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
