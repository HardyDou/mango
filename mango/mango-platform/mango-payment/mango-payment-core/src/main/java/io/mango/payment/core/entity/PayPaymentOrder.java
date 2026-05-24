package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mango_pay_payment_order")
public class PayPaymentOrder {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long bizOrderId;

    private String channelCode;

    private String channelOrderNo;

    private String payMethod;

    private String idempotencyKey;

    private Long amount;

    private String status;

    private String materialType;

    private String materialContent;

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
