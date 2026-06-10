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
@TableName("payment_exception_order")
public class PaymentExceptionOrderEntity extends AuditableEntity {

    private String exceptionNo;

    private String relatedOrderNo;

    private String exceptionType;

    private String severity;

    private String handleStatus;

    private String reason;

    private String handleAction;

    private String handleReason;

    private String handleResult;

    private String handleEvidence;

    private Long handlerId;

    private String handlerName;

    private LocalDateTime handleTime;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
