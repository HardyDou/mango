package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "支付接入应用视图")
public class PaymentApplicationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "应用 ID")
    private Long id;

    @Schema(description = "AppId，业务系统调用支付平台的应用身份")
    private String appId;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "应用密钥是否已配置")
    private Integer secretConfigured;

    @Schema(description = "密钥版本")
    private Integer secretVersion;

    @Schema(description = "密钥最后重置时间")
    private LocalDateTime secretLastResetTime;

    @Schema(description = "签名算法")
    private String signAlgorithm;

    @Schema(description = "IP 白名单开关：1-开启，0-关闭")
    private Integer ipWhitelistEnabled;

    @Schema(description = "IP 白名单")
    private String ipWhitelist;

    @Schema(description = "请求报文加密开关")
    private Integer payloadEncryptEnabled;

    @Schema(description = "通知重试策略")
    private String notifyRetryPolicy;

    @Schema(description = "是否示例应用：1-是，0-否")
    private Integer demoApp;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
