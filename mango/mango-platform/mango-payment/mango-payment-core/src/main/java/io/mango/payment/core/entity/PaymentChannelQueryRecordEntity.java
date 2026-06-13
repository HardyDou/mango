package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_channel_query_record")
public class PaymentChannelQueryRecordEntity extends AuditableEntity {

    private String queryNo;

    private String payOrderNo;

    private String channelTradeNo;

    private Long paymentOrderId;

    private Long businessOrderId;

    private Long channelId;

    private Long contractId;

    private String queryType;

    private String requestPayload;

    private String responsePayload;

    private String beforeStatus;

    private String channelStatus;

    private String resultStatus;

    private String processResult;

    private String processMessage;

    private LocalDateTime queryTime;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
