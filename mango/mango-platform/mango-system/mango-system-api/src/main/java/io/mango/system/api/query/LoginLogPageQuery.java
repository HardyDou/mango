package io.mango.system.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "登录日志分页查询条件")
public class LoginLogPageQuery extends PageQuery {

    @Schema(description = "关键字，支持用户名、登录IP模糊查询")
    private String keyword;

    @Schema(description = "登录状态：0-失败，1-成功")
    private Integer status;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
