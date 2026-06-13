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
@TableName("payment_business_order")
public class PaymentBusinessOrderEntity extends AuditableEntity {

    private String bizOrderNo;

    private String appCode;

    private String title;

    private Long subjectId;

    private Long amount;

    private Long paidAmount;

    private Long refundedAmount;

    private String currency;

    private String status;

    private LocalDateTime expireTime;

    private String notifyUrl;

    private String returnUrl;

    private String extendInfo;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
