package io.mango.integration.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 集成调用记录。
 */
@Data
@Schema(description = "集成调用记录")
public class IntegrationCallLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "实例编码")
    private String instanceCode;

    @Schema(description = "连接器编码")
    private String connectorCode;

    @Schema(description = "请求方法")
    private String httpMethod;

    @Schema(description = "请求路径")
    private String path;

    @Schema(description = "响应状态码，0 表示未建立连接")
    private Integer statusCode;

    @Schema(description = "耗时毫秒")
    private Long elapsedMs;

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "结果说明")
    private String message;

    @Schema(description = "调用时间")
    private LocalDateTime createdAt;
}
