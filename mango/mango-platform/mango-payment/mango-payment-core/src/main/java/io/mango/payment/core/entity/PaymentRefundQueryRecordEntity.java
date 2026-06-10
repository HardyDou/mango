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
@TableName("payment_refund_query_record")
public class PaymentRefundQueryRecordEntity extends AuditableEntity {

    private String queryNo;

    private String refundOrderNo;

    private String bizRefundNo;

    private String payOrderNo;

    private String channelRefundNo;

    private Long refundOrderId;

    private Long paymentOrderId;

    private Long businessOrderId;

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
