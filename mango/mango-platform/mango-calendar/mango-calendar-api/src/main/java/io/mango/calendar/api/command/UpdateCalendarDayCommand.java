package io.mango.calendar.api.command;

import io.mango.calendar.api.enums.CalendarDayType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "更新日历日期命令")
public class UpdateCalendarDayCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "日期 ID 不能为空")
    @Schema(description = "日期 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "日期类型不能为空")
    @Schema(description = "日期类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private CalendarDayType dayType;

    @Size(max = 128, message = "名称不能超过128个字符")
    @Schema(description = "日期名称，例如春节、调休补班")
    private String dayName;

    @Size(max = 64, message = "来源不能超过64个字符")
    @Schema(description = "数据来源，例如手工维护、国务院公告、导入")
    private String source;

    @Size(max = 256, message = "备注不能超过256个字符")
    @Schema(description = "备注")
    private String remark;
}
