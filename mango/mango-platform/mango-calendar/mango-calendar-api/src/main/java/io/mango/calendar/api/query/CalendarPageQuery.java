package io.mango.calendar.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "日历分页查询")
public class CalendarPageQuery extends PageQuery {

    @Schema(description = "关键词。支持日历编码、日历名称模糊搜索")
    private String keyword;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;
}
