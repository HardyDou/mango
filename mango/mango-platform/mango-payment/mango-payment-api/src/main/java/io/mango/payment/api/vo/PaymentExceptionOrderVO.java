package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "支付异常订单")
public class PaymentExceptionOrderVO {

    @Schema(description = "异常订单 ID")
    private Long id;

    @Schema(description = "异常单号")
    private String exceptionNo;

    @Schema(description = "关联订单号")
    private String relatedOrderNo;

    @Schema(description = "异常类型编码")
    private String exceptionType;

    @Schema(description = "异常类型名称")
    private String exceptionTypeName;

    @Schema(description = "异常级别编码")
    private String severity;

    @Schema(description = "异常级别名称")
    private String severityName;

    @Schema(description = "处理状态编码")
    private String handleStatus;

    @Schema(description = "处理状态名称")
    private String handleStatusName;

    @Schema(description = "异常原因")
    private String reason;

    @Schema(description = "处理动作")
    private String handleAction;

    @Schema(description = "处理原因")
    private String handleReason;

    @Schema(description = "处理结果")
    private String handleResult;

    @Schema(description = "处理凭据")
    private String handleEvidence;

    @Schema(description = "处理人 ID")
    private Long handlerId;

    @Schema(description = "处理人名称")
    private String handlerName;

    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
