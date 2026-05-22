package io.mango.calendar.api.command;

import io.mango.calendar.api.enums.CalendarDayType;
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
import java.time.LocalDate;
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
    @Valid
    @Schema(description = "导入日期列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Item> items;

    @Data
    @Schema(description = "导入日期项")
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        @NotNull(message = "日期不能为空")
        @Schema(description = "日期", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDate date;

        @NotNull(message = "日期类型不能为空")
        @Schema(description = "日期类型", requiredMode = Schema.RequiredMode.REQUIRED)
        private CalendarDayType dayType;

        @Size(max = 128, message = "名称不能超过128个字符")
        @Schema(description = "日期名称")
        private String dayName;

        @Size(max = 64, message = "来源不能超过64个字符")
        @Schema(description = "数据来源")
        private String source;

        @Size(max = 256, message = "备注不能超过256个字符")
        @Schema(description = "备注")
        private String remark;
    }
}
