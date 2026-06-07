package io.mango.job.api.vo;

import io.mango.job.api.enums.JobHandleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Mango Job Worker 执行结果。
 */
@Data
@Schema(description = "Mango Job Worker 执行结果")
public class MangoJobWorkerExecuteResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "执行状态")
    private JobHandleStatus status;

    @Schema(description = "结果摘要")
    private String message;

    @Schema(description = "结果数据 JSON")
    private String result;

    @Schema(description = "Worker 地址")
    private String workerAddress;

    @Schema(description = "执行日志")
    private List<MangoJobWorkerExecutionLogVO> logs;

    public MangoJobWorkerExecuteResultVO() {
        this.logs = new ArrayList<>();
    }
}
