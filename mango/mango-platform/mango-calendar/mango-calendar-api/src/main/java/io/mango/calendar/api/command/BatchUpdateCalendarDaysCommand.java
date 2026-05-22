package io.mango.calendar.api.command;

import io.mango.calendar.api.enums.CalendarDayType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "批量更新日历日期命令")
public class BatchUpdateCalendarDaysCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "日期 ID 不能为空")
    @Schema(description = "日期 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;

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
