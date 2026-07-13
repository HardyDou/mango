package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_channel_bill_batch")
public class PaymentChannelBillBatchEntity extends PaymentBaseEntity {

    private String batchNo;

    private Long reconciliationId;

    private String channelCode;

    private LocalDate billDate;

    private String fileDigest;

    private Long billFileId;

    private String billFileName;

    private Integer totalCount;

    private Long totalAmount;

    private Long totalFee;

    private String importStatus;

    private Long importerId;

    private String importerName;

    private LocalDateTime importTime;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
