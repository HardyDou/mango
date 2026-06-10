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
@TableName("payment_offline_collection_voucher")
public class PaymentOfflineCollectionVoucherEntity extends AuditableEntity {

    private Long offlineCollectionId;

    private String offlineCollectionNo;

    private String payOrderNo;

    private String voucherFileId;

    private String uploadSource;

    private Long uploaderId;

    private String uploaderName;

    private LocalDateTime uploadTime;

    private String reviewStatus;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
