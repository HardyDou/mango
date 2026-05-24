package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mango_pay_refund_order")
public class PayRefundOrder {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long bizOrderId;

    private Long paymentOrderId;

    private String merchantRefundNo;

    private String channelRefundNo;

    private String idempotencyKey;

    private Long refundAmount;

    private String status;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
