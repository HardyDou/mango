package io.mango.integration.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.time.LocalDateTime;

/**
 * 连接器定义结果。
 */
@Data
@Schema(description = "连接器定义")
public class IntegrationConnectorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "连接器编码")
    private String connectorCode;

    @Schema(description = "连接器名称")
    private String connectorName;

    @Schema(description = "用途分类")
    private String category;

    @Schema(description = "厂商标识")
    private String provider;

    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;

    @Schema(description = "来源：MANUAL-页面维护，DECLARED-资源声明")
    private String source;

    @Schema(description = "配置规范JSON Schema")
    private String configSchema;

    @Schema(description = "认证规范JSON")
    private String authSchema;

    @Schema(description = "能力清单JSON")
    private String capabilitiesJson;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "文档地址")
    private String docsUrl;

    @Schema(description = "被引用实例数")
    private Long instanceCount;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
