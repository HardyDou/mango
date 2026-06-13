package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "收银台应用视图")
public class PaymentCashierApplicationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "支付应用 ID")
    private Long id;

    @Schema(description = "支付应用 AppId")
    private String appId;

    @Schema(description = "支付应用名称")
    private String appName;
}
