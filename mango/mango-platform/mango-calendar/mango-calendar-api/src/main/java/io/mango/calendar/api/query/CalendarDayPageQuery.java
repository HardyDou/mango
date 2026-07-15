package io.mango.calendar.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "日历日期分页查询")
public class CalendarDayPageQuery extends PageQuery {

    @Schema(description = "日历编码")
    @Size(max = 64, message = "日历编码不能超过64个字符")
    private String calendarCode;

    @Schema(description = "年度，例如 2026")
    @Min(value = 1900, message = "年度不能早于1900")
    @Max(value = 2100, message = "年度不能晚于2100")
    private Integer year;

    @Schema(description = "开始日期")
    @Pattern(regexp = "^$|\\d{4}-\\d{2}-\\d{2}$", message = "开始日期格式必须为yyyy-MM-dd")
    private String startDate;

    @Schema(description = "结束日期")
    @Pattern(regexp = "^$|\\d{4}-\\d{2}-\\d{2}$", message = "结束日期格式必须为yyyy-MM-dd")
    private String endDate;

    @Schema(description = "日期类型")
    @Pattern(regexp = "^$|WORKDAY|RESTDAY|LEGAL_HOLIDAY|ADJUSTED_WORKDAY|TEMP_CLOSED_DAY|TEMP_OPEN_DAY|HOLIDAY|CUSTOM_CLOSED|CUSTOM_OPEN$",
            message = "日期类型不正确")
    private String dayType;

    @Schema(description = "是否工作日")
    @Pattern(regexp = "^$|true|false$", flags = Pattern.Flag.CASE_INSENSITIVE, message = "是否工作日只能是true或false")
    private String workday;

    @Schema(description = "启用状态：1-启用，0-停用")
    @Min(value = 0, message = "启用状态只能是0或1")
    @Max(value = 1, message = "启用状态只能是0或1")
    private Integer enabled;

    @Schema(description = "关键词。支持日期名称、来源、备注模糊搜索")
    @Size(max = 128, message = "关键词不能超过128个字符")
    private String keyword;
}
