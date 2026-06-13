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
@TableName("payment_offline_collection")
public class PaymentOfflineCollectionEntity extends AuditableEntity {

    private String offlineCollectionNo;

    private Long paymentOrderId;

    private String payOrderNo;

    private Long businessOrderId;

    private String bizOrderNo;

    private Long channelId;

    private String channelCode;

    private Long contractId;

    private Long contractCapabilityId;

    private Long subjectId;

    private String subjectName;

    private Long bankAccountId;

    private String accountName;

    private String accountNoMask;

    private String bankName;

    private Long amount;

    private String currency;

    private Long transferAmount;

    private String voucherFileIds;

    private LocalDateTime submittedTime;

    private String submitRemark;

    private Long confirmedAmount;

    private Long confirmedBy;

    private String confirmedByName;

    private String confirmRemark;

    private String reconciliationCode;

    private String transferRemark;

    private Integer voucherCount;

    private String collectionStatus;

    private LocalDateTime expireTime;

    private LocalDateTime confirmedTime;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
