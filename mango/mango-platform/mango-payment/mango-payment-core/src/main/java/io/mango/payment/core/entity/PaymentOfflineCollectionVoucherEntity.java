package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_offline_collection_voucher")
public class PaymentOfflineCollectionVoucherEntity extends PaymentBaseEntity {

    private Long offlineCollectionId;

    private String offlineCollectionNo;

    private String payOrderNo;

    private String voucherFileId;

    private String uploadSource;

    private Long uploaderId;

    private String uploaderName;

    private LocalDateTime uploadTime;

    private String reviewStatus;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
