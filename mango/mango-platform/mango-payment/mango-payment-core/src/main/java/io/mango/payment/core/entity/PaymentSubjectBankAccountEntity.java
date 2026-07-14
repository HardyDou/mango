package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_subject_bank_account")
public class PaymentSubjectBankAccountEntity extends PaymentBaseEntity {

    private Long subjectId;

    private String accountName;

    private String accountNo;

    private String bankName;

    private String bankBranchName;

    private String bankCode;

    private String accountType;

    private Integer defaultAccount;

    private Integer status;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
