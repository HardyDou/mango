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
@TableName("payment_channel_bill_fetch_batch")
public class PaymentChannelBillFetchBatchEntity extends AuditableEntity {

    private Long sourceId;

    private String batchNo;

    private Long reconciliationId;

    private String channelCode;

    private String fetchMode;

    private LocalDate billDate;

    private LocalDateTime requestStartTime;

    private LocalDateTime requestEndTime;

    private String requestCursor;

    private Integer requestPage;

    private Integer pageSize;

    private String responseDigest;

    private Integer totalCount;

    private String fetchStatus;

    private String fetchResult;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime fetchStartTime;

    private LocalDateTime fetchEndTime;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
