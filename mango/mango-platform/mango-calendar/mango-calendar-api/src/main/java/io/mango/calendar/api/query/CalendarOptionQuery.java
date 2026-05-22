package io.mango.calendar.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "日历选项查询")
public class CalendarOptionQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关键词。支持日历编码、日历名称模糊搜索")
    private String keyword;

    @Schema(description = "是否包含停用日历")
    private Boolean includeDisabled;
}
