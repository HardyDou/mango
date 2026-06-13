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
@TableName("payment_channel_certificate_rotation_record")
public class PaymentChannelCertificateRotationRecordEntity extends AuditableEntity {

    private Long contractId;

    private Long contractCapabilityId;

    private String certificateFieldCode;

    private Long oldCertificateFileId;

    private Long newCertificateFileId;

    private LocalDateTime oldCertificateExpireTime;

    private LocalDateTime newCertificateExpireTime;

    private String rotateReason;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime rotateTime;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
