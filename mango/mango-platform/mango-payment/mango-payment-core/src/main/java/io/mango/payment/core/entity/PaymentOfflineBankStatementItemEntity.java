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
@TableName("payment_offline_bank_statement_item")
public class PaymentOfflineBankStatementItemEntity extends AuditableEntity {

    private Long batchId;

    private String batchNo;

    private Integer rowNo;

    private String bankStatementNo;

    private String bankAccountNoMask;

    private String bankName;

    private LocalDateTime tradeTime;

    private LocalDate tradeDate;

    private Long amount;

    private String currency;

    private String counterpartyName;

    private String counterpartyAccountNoMask;

    private String summary;

    private String remark;

    private String reconciliationCode;

    private Long matchedOfflineCollectionId;

    private String matchedOfflineCollectionNo;

    private String matchedPayOrderNo;

    private String matchStatus;

    private String matchMessage;

    private LocalDateTime confirmedTime;

    private Long confirmedBy;

    private String confirmedByName;

    private String confirmRemark;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
