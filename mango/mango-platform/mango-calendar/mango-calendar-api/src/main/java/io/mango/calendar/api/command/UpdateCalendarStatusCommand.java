package io.mango.calendar.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "更新日历状态命令")
public class UpdateCalendarStatusCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "日历 ID 不能为空")
    @Positive(message = "日历 ID 必须大于0")
    @Schema(description = "日历 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能是0或1")
    @Max(value = 1, message = "状态只能是0或1")
    @Schema(description = "状态：1-启用，0-停用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
