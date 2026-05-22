package io.mango.calendar.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "日历选项")
public class CalendarOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日历编码")
    private String calendarCode;

    @Schema(description = "日历名称")
    private String calendarName;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;
}
