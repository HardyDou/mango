package io.mango.job.api.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 手动触发 Job 命令。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "手动触发 Job 命令")
public class TriggerMangoJobCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "任务 ID 不能为空")
    @Schema(description = "任务 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long jobId;

    @Size(max = 128, message = "触发批次号不能超过128个字符")
    @Schema(description = "触发批次号。为空时由服务端生成")
    private String triggerBatchNo;

    @Schema(description = "本次触发参数 JSON")
    private String paramValue;
}
