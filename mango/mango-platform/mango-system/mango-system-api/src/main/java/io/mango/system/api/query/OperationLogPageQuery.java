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
@Schema(description = "操作日志分页查询条件")
public class OperationLogPageQuery extends PageQuery {

    @Schema(description = "关键字，支持用户名、操作名称、请求路径模糊查询")
    @Size(max = 200, message = "查询关键字长度不正确")
    private String keyword;

    @Schema(description = "用户名，支持模糊查询")
    @Size(max = 64, message = "用户名长度不正确")
    private String username;

    @Schema(description = "操作状态：0-失败，1-成功")
    @Max(value = Integer.MAX_VALUE, message = "状态不正确")
    private Integer status;

    @Schema(description = "开始时间")
    @PastOrPresent(message = "开始时间不能晚于当前时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @PastOrPresent(message = "结束时间不能晚于当前时间")
    private LocalDateTime endTime;
}
