package io.mango.calendar.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Schema(description = "公历日期查询")
public class SolarDateQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "日期不能为空")
    @Schema(description = "公历日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate date;
}
