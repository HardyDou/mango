package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_channel_field_template")
public class PaymentChannelFieldTemplateEntity extends AuditableEntity {

    private Long channelId;

    private String fieldCode;

    private String fieldLabel;

    private String componentType;

    private String dataType;

    private Integer requiredFlag;

    private Integer sensitiveFlag;

    private Integer encryptedFlag;

    private Integer maskedFlag;

    private Integer fileReferenceFlag;

    private String optionJson;

    private String validationJson;

    private String fieldGroup;

    private Integer sort;

    private Integer status;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
