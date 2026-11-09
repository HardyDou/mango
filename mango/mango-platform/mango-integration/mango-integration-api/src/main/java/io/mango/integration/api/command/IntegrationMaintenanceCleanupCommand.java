package io.mango.integration.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 集成日志清理命令。
 */
@Data
@Schema(description = "集成日志清理命令")
public class IntegrationMaintenanceCleanupCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "清理对象不能为空")
    @Pattern(regexp = "CALL_LOG|ACCESS_LOG|ROTATION", message = "清理对象只能是CALL_LOG、ACCESS_LOG或ROTATION")
    @Schema(description = "清理对象：CALL_LOG-调用记录，ACCESS_LOG-凭证读取记录，ROTATION-轮换记录")
    private String resource;

    @NotNull(message = "保留天数不能为空")
    @Min(value = 1, message = "保留天数不能小于1")
    @Max(value = 3650, message = "保留天数不能超过3650")
    @Schema(description = "保留天数，早于该天数的记录会被删除")
    private Integer retentionDays;
}
