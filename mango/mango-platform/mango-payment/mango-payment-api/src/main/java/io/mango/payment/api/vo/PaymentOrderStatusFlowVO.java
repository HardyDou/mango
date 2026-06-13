package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "支付订单状态流转记录")
public class PaymentOrderStatusFlowVO {

    @Schema(description = "变更前状态编码")
    private String fromStatus;

    @Schema(description = "变更后状态编码")
    private String toStatus;

    @Schema(description = "状态编码")
    private String statusCode;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "发生时间")
    private LocalDateTime happenTime;

    @Schema(description = "触发来源编码")
    private String triggerSource;

    @Schema(description = "流转来源")
    private String source;

    @Schema(description = "触发单号")
    private String triggerNo;

    @Schema(description = "操作人 ID")
    private Long operatorId;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "说明")
    private String remark;
}
