package io.mango.calendar.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "日历")
public class CalendarVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日历 ID")
    private Long id;

    @Schema(description = "日历编码")
    private String calendarCode;

    @Schema(description = "日历名称")
    private String calendarName;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
