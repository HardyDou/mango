package io.mango.calendar.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "导入日历日期命令")
public class ImportCalendarDaysCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "日历编码不能为空")
    @Schema(description = "日历编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String calendarCode;

    @NotNull(message = "年度不能为空")
    @Min(value = 1900, message = "年度不能早于1900")
    @Max(value = 2100, message = "年度不能晚于2100")
    @Schema(description = "年度", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer year;

    @NotEmpty(message = "导入日期不能为空")
    @Size(max = 366, message = "一次最多导入366个日期")
    @Valid
    @Schema(description = "导入日期列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ImportCalendarDayCommand> items;
}
