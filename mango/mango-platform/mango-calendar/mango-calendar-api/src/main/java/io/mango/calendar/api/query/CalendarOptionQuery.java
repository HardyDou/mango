package io.mango.calendar.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "日历选项查询")
public class CalendarOptionQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关键词。支持日历编码、日历名称模糊搜索")
    @Size(max = 128, message = "关键词不能超过128个字符")
    private String keyword;

    @Schema(description = "是否包含停用日历")
    @NotNull(message = "是否包含停用日历不能为空")
    private Boolean includeDisabled = false;
}
