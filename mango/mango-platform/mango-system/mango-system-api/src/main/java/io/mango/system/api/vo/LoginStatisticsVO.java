package io.mango.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginStatisticsVO {
    @Schema(description = "登录总次数")
    private long totalCount;
    @Schema(description = "登录成功次数")
    private long successCount;
    @Schema(description = "登录失败次数")
    private long failCount;
    @Schema(description = "今日登录次数")
    private long todayCount;
    @Schema(description = "本周登录次数")
    private long weekCount;
    @Schema(description = "本月登录次数")
    private long monthCount;
}
