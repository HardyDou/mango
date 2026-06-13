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
@TableName("payment_application")
public class PaymentApplication extends AuditableEntity {

    private String appId;

    private String appName;

    private String appSecret;

    private Integer secretConfigured;

    private Integer secretVersion;

    private LocalDateTime secretLastResetTime;

    private String signAlgorithm;

    private Integer ipWhitelistEnabled;

    private String ipWhitelist;

    private Integer payloadEncryptEnabled;

    private String notifyRetryPolicy;

    private Integer demoApp;

    private Integer status;

    private Long tenantId;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
