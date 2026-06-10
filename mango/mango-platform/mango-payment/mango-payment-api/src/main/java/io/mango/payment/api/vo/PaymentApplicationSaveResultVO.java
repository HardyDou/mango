package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "支付接入应用保存结果")
public class PaymentApplicationSaveResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "应用 ID")
    private Long id;

    @Schema(description = "AppId，业务系统调用支付平台的应用身份")
    private String appId;

    @Schema(description = "本次生成的应用密钥。仅在创建或启用报文加密时返回一次，列表和详情不返回")
    private String appSecret;

    @Schema(description = "本次是否生成应用密钥：1-是，0-否")
    private Integer secretGenerated;
}
