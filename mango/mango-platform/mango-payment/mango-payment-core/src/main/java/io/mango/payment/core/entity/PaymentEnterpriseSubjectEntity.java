package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_enterprise_subject")
public class PaymentEnterpriseSubjectEntity extends PaymentBaseEntity {

    private String subjectName;

    private String creditCode;

    private String creditCodeHash;

    private String bankAccountNo;

    private String bankName;

    private Long licenseFileId;

    private Integer status;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
