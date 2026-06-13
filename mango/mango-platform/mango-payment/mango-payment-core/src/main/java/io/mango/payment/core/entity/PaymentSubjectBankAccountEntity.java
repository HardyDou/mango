package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_subject_bank_account")
public class PaymentSubjectBankAccountEntity extends AuditableEntity {

    private Long subjectId;

    private String accountName;

    private String accountNo;

    private String bankName;

    private String bankBranchName;

    private String bankCode;

    private String accountType;

    private Integer defaultAccount;

    private Integer status;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
