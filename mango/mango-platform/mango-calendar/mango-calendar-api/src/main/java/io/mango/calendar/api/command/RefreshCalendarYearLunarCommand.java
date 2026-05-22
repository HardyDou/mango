package io.mango.calendar.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "刷新年度农历信息")
public class RefreshCalendarYearLunarCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "日历编码不能为空")
    @Schema(description = "日历编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String calendarCode;

    @NotNull(message = "年度不能为空")
    @Min(value = 1900, message = "年度不能早于1900")
    @Max(value = 2100, message = "年度不能晚于2100")
    @Schema(description = "年度，例如 2026", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer year;
}
