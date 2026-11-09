package io.mango.integration.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 集成实例连接测试结果。
 */
@Data
@Schema(description = "集成实例连接测试结果")
public class IntegrationConnectionTestVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否连通")
    private Boolean success;

    @Schema(description = "响应状态码，0 表示未建立连接")
    private Integer statusCode;

    @Schema(description = "耗时毫秒")
    private Long elapsedMs;

    @Schema(description = "结果说明，不含凭证与响应正文")
    private String message;
}
