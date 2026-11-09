package io.mango.integration.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 连接实例声明，供业务域读取配置与授权能力。
 */
@Data
@Schema(description = "连接实例声明")
public class IntegrationInstanceDeclarationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "实例编码")
    private String instanceCode;

    @Schema(description = "连接器编码")
    private String connectorCode;

    @Schema(description = "环境：PROD/TEST")
    private String environment;

    @Schema(description = "配置值JSON")
    private String configValuesJson;

    @Schema(description = "凭证编码")
    private String credentialCode;

    @Schema(description = "已授权能力JSON数组")
    private String capabilitiesJson;

    @Schema(description = "回调路径")
    private String callbackPath;

    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;
}
