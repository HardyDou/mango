package io.mango.payment.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "支付通道账单获取批次")
public class PaymentChannelBillFetchBatchVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "获取批次 ID")
    private Long id;
    @Schema(description = "账单源 ID")
    private Long sourceId;
    @Schema(description = "获取批次号")
    private String batchNo;
    @Schema(description = "对账批次 ID")
    private Long reconciliationId;
    @Schema(description = "对账批次号")
    private String reconciliationNo;
    @Schema(description = "支付通道编码")
    private String channelCode;
    @Schema(description = "账单获取模式")
    private String fetchMode;
    @Schema(description = "账单获取模式名称")
    private String fetchModeName;
    @Schema(description = "账单日期")
    private LocalDate billDate;
    @Schema(description = "请求开始时间")
    private LocalDateTime requestStartTime;
    @Schema(description = "请求结束时间")
    private LocalDateTime requestEndTime;
    @Schema(description = "请求游标")
    private String requestCursor;
    @Schema(description = "请求页码")
    private Integer requestPage;
    @Schema(description = "请求分页大小")
    private Integer pageSize;
    @Schema(description = "响应摘要")
    private String responseDigest;
    @Schema(description = "账单总条数")
    private Integer totalCount;
    @Schema(description = "获取状态编码")
    private String fetchStatus;
    @Schema(description = "获取状态名称")
    private String fetchStatusName;
    @Schema(description = "获取结果说明")
    private String fetchResult;
    @Schema(description = "操作人 ID")
    private Long operatorId;
    @Schema(description = "操作人名称")
    private String operatorName;
    @Schema(description = "获取开始时间")
    private LocalDateTime fetchStartTime;
    @Schema(description = "获取结束时间")
    private LocalDateTime fetchEndTime;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
