package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "支付通道能力视图")
public class PaymentChannelCapabilityVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "通道能力 ID")
    private Long id;

    @Schema(description = "通道 ID")
    private Long channelId;

    @Schema(description = "通道名称")
    private String channelName;

    @Schema(description = "标准支付方式编码")
    private String methodCode;

    @Schema(description = "标准支付方式名称")
    private String methodName;

    @Schema(description = "终端类型")
    private String terminalType;

    @Schema(description = "内部路由域")
    private String environment;

    @Schema(description = "是否支持退款")
    private Integer supportsRefund;

    @Schema(description = "是否支持查单")
    private Integer supportsQuery;

    @Schema(description = "是否支持关单")
    private Integer supportsClose;

    @Schema(description = "是否支持账单")
    private Integer supportsBill;

    @Schema(description = "是否支持对账")
    private Integer supportsReconcile;

    @Schema(description = "最小金额，单位分")
    private Long minAmount;

    @Schema(description = "最大金额，单位分")
    private Long maxAmount;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
