package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 前端运行单元健康检查结果。
 */
@Data
@Schema(description = "前端运行单元健康检查结果")
public class FrontendAppHealthCheckVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "运行单元编码")
    private String runtimeCode;

    @Schema(description = "健康检查目标地址")
    private String targetUrl;

    @Schema(description = "是否连通")
    private Boolean success;

    @Schema(description = "目标响应状态码，未建立连接时为空")
    private Integer statusCode;

    @Schema(description = "耗时，单位毫秒")
    private Long elapsedMs;

    @Schema(description = "结果说明，不包含凭证和响应正文")
    private String message;

    @Schema(description = "检查时间")
    private LocalDateTime checkedAt;
}
