package io.mango.calendar.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Schema(description = "日期区间查询")
public class DateRangeQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "日历编码不能为空")
    @Schema(description = "日历编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String calendarCode;

    @NotNull(message = "开始日期不能为空")
    @Schema(description = "开始日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    @Schema(description = "结束日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate endDate;
}
