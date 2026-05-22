package io.mango.calendar.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "农历转公历查询")
public class LunarDateQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "农历年不能为空")
    @Min(value = 1900, message = "农历年不能早于1900")
    @Max(value = 2100, message = "农历年不能晚于2100")
    @Schema(description = "农历年，例如 2025", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer lunarYear;

    @NotNull(message = "农历月不能为空")
    @Min(value = 1, message = "农历月取值为 1-12")
    @Max(value = 12, message = "农历月取值为 1-12")
    @Schema(description = "农历月，1-12", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer lunarMonth;

    @NotNull(message = "农历日不能为空")
    @Min(value = 1, message = "农历日取值为 1-30")
    @Max(value = 30, message = "农历日取值为 1-30")
    @Schema(description = "农历日，1-30", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer lunarDay;

    @Schema(description = "是否闰月")
    private Boolean leapMonth;
}
