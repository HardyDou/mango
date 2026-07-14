package io.mango.payment.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_application")
public class PaymentApplicationEntity extends PaymentBaseEntity {

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

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag = 0;
}
