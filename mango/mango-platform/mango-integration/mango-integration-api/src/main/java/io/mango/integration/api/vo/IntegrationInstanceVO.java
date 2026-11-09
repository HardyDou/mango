package io.mango.integration.api.vo;

import io.mango.integration.api.enums.IntegrationInstanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 连接实例结果。
 */
@Data
@Schema(description = "集成连接实例")
public class IntegrationInstanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "实例编码")
    private String instanceCode;

    @Schema(description = "实例名称")
    private String instanceName;

    @Schema(description = "连接器编码")
    private String connectorCode;

    @Schema(description = "环境：PROD/TEST")
    private String environment;

    @Schema(description = "配置值JSON")
    private String configValuesJson;

    @Schema(description = "凭证编码")
    private String credentialCode;

    @Schema(description = "已授权能力JSON")
    private String capabilitiesJson;

    @Schema(description = "回调路径")
    private String callbackPath;

    @Schema(description = "状态")
    private IntegrationInstanceStatus status;

    @Schema(description = "所属连接器是否启用")
    private Boolean connectorEnabled;

    @Schema(description = "责任人")
    private String owner;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
