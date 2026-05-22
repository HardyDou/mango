package io.mango.calendar.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "月份第 N 个工作日查询")
public class NthWorkdayOfMonthQuery extends MonthQuery {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "序号不能为空")
    @Min(value = 1, message = "序号必须大于等于 1")
    @Max(value = 31, message = "序号不能大于 31")
    @Schema(description = "第 N 个工作日", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer nth;
}
