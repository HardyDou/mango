package io.mango.job.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Mango Job Worker 执行日志。
 */
@Data
@Schema(description = "Mango Job Worker 执行日志")
public class MangoJobWorkerExecutionLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日志级别")
    private String level;

    @Schema(description = "日志来源或 logger 名称")
    private String loggerName;

    @Schema(description = "日志内容")
    private String content;
}
