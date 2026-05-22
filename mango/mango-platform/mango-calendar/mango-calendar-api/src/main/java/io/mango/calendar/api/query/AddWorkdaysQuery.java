package io.mango.calendar.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Schema(description = "工作日偏移查询")
public class AddWorkdaysQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "日历编码不能为空")
    @Schema(description = "日历编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String calendarCode;

    @NotNull(message = "起始日期不能为空")
    @Schema(description = "起始日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate sourceDate;

    @NotNull(message = "工作日偏移量不能为空")
    @Schema(description = "工作日偏移量，可为负数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer amount;

    @Schema(description = "起始日期是工作日时是否计入")
    private Boolean includeSource;
}
