package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_openapi_nonce")
public class PaymentOpenApiNonceEntity extends PaymentBaseEntity {

    private String appId;

    private String nonce;

    private LocalDateTime expireTime;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
