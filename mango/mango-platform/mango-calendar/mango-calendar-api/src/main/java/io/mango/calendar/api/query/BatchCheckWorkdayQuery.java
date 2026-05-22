package io.mango.calendar.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "批量校验工作日查询")
public class BatchCheckWorkdayQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "日历编码不能为空")
    @Schema(description = "日历编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String calendarCode;

    @NotEmpty(message = "日期列表不能为空")
    @Schema(description = "日期列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<LocalDate> dates;
}
