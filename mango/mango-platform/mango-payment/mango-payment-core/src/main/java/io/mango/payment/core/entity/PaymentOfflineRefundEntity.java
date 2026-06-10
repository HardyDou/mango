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
@TableName("payment_offline_refund_process")
public class PaymentOfflineRefundEntity extends AuditableEntity {

    private String offlineRefundNo;

    private Long offlineCollectionId;

    private String offlineCollectionNo;

    private Long refundOrderId;

    private Long paymentOrderId;

    private String payOrderNo;

    private Long businessOrderId;

    private String bizOrderNo;

    private Long channelId;

    private String channelCode;

    private Long refundAmount;

    private String currency;

    private String refundAccountName;

    private String refundAccountNoMask;

    private String refundBankName;

    private String refundVoucherFileIds;

    private Integer refundVoucherCount;

    private String reason;

    private String remark;

    private String refundStatus;

    private LocalDateTime refundedTime;

    private Long operatorId;

    private String operatorName;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
