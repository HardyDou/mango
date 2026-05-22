package io.mango.calendar.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "年度节气查询")
public class SolarTermYearQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "年度不能为空")
    @Min(value = 1900, message = "年度不能早于1900")
    @Max(value = 2100, message = "年度不能晚于2100")
    @Schema(description = "年度，例如 2026", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer year;
}
