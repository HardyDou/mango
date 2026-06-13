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
@TableName("payment_reconciliation")
public class PaymentReconciliationEntity extends AuditableEntity {

    private String reconciliationNo;

    private String channelCode;

    private LocalDate billDate;

    private Integer totalCount;

    private Long totalAmount;

    private Long totalFee;

    private String matchStatus;

    private Long billFileId;

    private String billFileName;

    private String fileDigest;

    private Long importerId;

    private String importerName;

    private LocalDateTime importTime;

    private String reconcileResult;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
