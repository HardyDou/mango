package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "支付操作审计")
public class PaymentOperationAuditVO {

    @Schema(description = "审计记录 ID")
    private Long id;

    @Schema(description = "操作人 ID")
    private Long operatorId;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "操作动作编码")
    private String operationAction;

    @Schema(description = "资源类型编码")
    private String resourceType;

    @Schema(description = "资源标识")
    private String resourceId;

    @Schema(description = "操作结果编码")
    private String operationResult;

    @Schema(description = "操作时间")
    private LocalDateTime operationTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
