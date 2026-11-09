package io.mango.integration.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 连接实例保存命令。
 */
@Data
@Schema(description = "连接实例保存命令")
public class IntegrationInstanceCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "实例编码不能为空")
    @Size(max = 64, message = "实例编码最多64个字符")
    @Schema(description = "实例编码")
    private String instanceCode;

    @NotBlank(message = "实例名称不能为空")
    @Size(max = 100, message = "实例名称最多100个字符")
    @Schema(description = "实例名称")
    private String instanceName;

    @NotBlank(message = "连接器编码不能为空")
    @Size(max = 64, message = "连接器编码最多64个字符")
    @Schema(description = "连接器编码")
    private String connectorCode;

    @Size(max = 16, message = "环境最多16个字符")
    @Schema(description = "环境：PROD/TEST")
    private String environment;

    @Size(max = 8192, message = "配置值JSON最多8192个字符")
    @Schema(description = "配置值JSON对象，按连接器配置规范校验")
    private String configValuesJson;

    @Size(max = 64, message = "凭证编码最多64个字符")
    @Schema(description = "凭证编码")
    private String credentialCode;

    @Size(max = 2048, message = "能力清单JSON最多2048个字符")
    @Schema(description = "已授权能力JSON数组")
    private String capabilitiesJson;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;

    @Size(max = 64, message = "责任人最多64个字符")
    @Schema(description = "责任人")
    private String owner;

    @Size(max = 500, message = "备注最多500个字符")
    @Schema(description = "备注")
    private String remark;
}
