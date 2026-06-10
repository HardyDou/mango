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
@TableName("payment_offline_collection_match")
public class PaymentOfflineCollectionMatchEntity extends AuditableEntity {

    private Long offlineCollectionId;

    private String offlineCollectionNo;

    private Long bankStatementItemId;

    private String bankStatementNo;

    private String payOrderNo;

    private String matchRule;

    private String matchStatus;

    private String differenceType;

    private String matchMessage;

    private LocalDateTime confirmedTime;

    private Long confirmedBy;

    private String confirmedByName;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
