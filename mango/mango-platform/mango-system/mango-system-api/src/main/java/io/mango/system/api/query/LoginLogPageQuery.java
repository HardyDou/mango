package io.mango.system.api.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import jakarta.validation.constraints.PastOrPresent;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "登录日志分页查询条件")
public class LoginLogPageQuery extends PageQuery {

    @Schema(description = "关键字，支持用户名、登录IP模糊查询")
    @Size(max = 200, message = "查询关键字长度不正确")
    private String keyword;

    @Schema(description = "登录状态：0-失败，1-成功")
    @Max(value = Integer.MAX_VALUE, message = "状态不正确")
    private Integer status;

    @Schema(description = "开始时间")
    @PastOrPresent(message = "开始时间不能晚于当前时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @PastOrPresent(message = "结束时间不能晚于当前时间")
    private LocalDateTime endTime;
}
