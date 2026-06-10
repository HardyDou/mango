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
@TableName("payment_offline_bank_statement_batch")
public class PaymentOfflineBankStatementBatchEntity extends AuditableEntity {

    private String batchNo;

    private String bankAccountNoMask;

    private String bankName;

    private Long statementFileId;

    private String statementFileName;

    private String fileDigest;

    private Integer totalCount;

    private Integer matchedCount;

    private Integer confirmedCount;

    private Integer differenceCount;

    private String batchStatus;

    private Long importerId;

    private String importerName;

    private LocalDateTime importTime;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
