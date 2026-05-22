package io.mango.calendar.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Schema(description = "节气")
public class SolarTermVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "节气名称")
    private String name;

    @Schema(description = "公历日期")
    private LocalDate date;
}
