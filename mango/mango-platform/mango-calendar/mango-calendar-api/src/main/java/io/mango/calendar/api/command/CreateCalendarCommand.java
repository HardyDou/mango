package io.mango.calendar.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "创建日历命令")
public class CreateCalendarCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "日历编码不能为空")
    @Size(max = 64, message = "日历编码不能超过64个字符")
    @Schema(description = "日历编码，创建后不建议修改", requiredMode = Schema.RequiredMode.REQUIRED)
    private String calendarCode;

    @NotBlank(message = "日历名称不能为空")
    @Size(max = 128, message = "日历名称不能超过128个字符")
    @Schema(description = "日历名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String calendarName;
}
