package io.mango.calendar.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Schema(description = "农历日期信息")
public class LunarDayInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "公历日期")
    private LocalDate solarDate;

    @Schema(description = "农历年")
    private Integer lunarYear;

    @Schema(description = "农历月")
    private Integer lunarMonth;

    @Schema(description = "农历日")
    private Integer lunarDay;

    @Schema(description = "是否农历闰月")
    private boolean lunarLeapMonth;

    @Schema(description = "农历中文日期")
    private String lunarText;

    @Schema(description = "干支纪年")
    private String ganzhiYear;

    @Schema(description = "生肖")
    private String zodiac;

    @Schema(description = "节气")
    private String solarTerm;
}
