package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 租户收银台视图。
 */
@Data
@Schema(description = "租户收银台视图")
public class PaymentTenantCashierVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "租户名称")
    private String tenantName;

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "收银台编码")
    private String cashierCode;

    @Schema(description = "收银台名称")
    private String cashierName;

    @Schema(description = "启用支付方式")
    private List<String> enabledMethods;

    @Schema(description = "默认支付方式")
    private String defaultMethod;

    @Schema(description = "订单过期分钟数")
    private Integer expireMinutes;

    @Schema(description = "日限额，单位分")
    private Long dailyLimit;
}
