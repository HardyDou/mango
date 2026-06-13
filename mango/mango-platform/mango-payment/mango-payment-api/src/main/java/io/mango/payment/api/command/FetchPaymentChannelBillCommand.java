package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "发起通道账单获取命令")
public class FetchPaymentChannelBillCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "账单获取源 ID 不能为空")
    @Schema(description = "账单获取源 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sourceId;

    @NotNull(message = "账单日期不能为空")
    @Schema(description = "账单日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate billDate;

    @Schema(description = "请求开始时间。HTTP 获取默认使用账单日 00:00:00")
    private LocalDateTime startTime;

    @Schema(description = "请求结束时间。HTTP 获取默认使用账单日次日 00:00:00")
    private LocalDateTime endTime;
}
