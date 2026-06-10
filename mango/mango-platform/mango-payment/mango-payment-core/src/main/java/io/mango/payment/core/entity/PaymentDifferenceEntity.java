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
@TableName("payment_difference")
public class PaymentDifferenceEntity extends AuditableEntity {

    private String differenceNo;

    private Long reconciliationId;

    private String relatedOrderNo;

    private String differenceType;

    private Long differenceAmount;

    private String processStatus;

    private String processResult;

    private String processAction;

    private String processReason;

    private String processEvidence;

    private Long adjustFlowId;

    private String adjustFlowNo;

    private Long processorId;

    private String processorName;

    private LocalDateTime processTime;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
