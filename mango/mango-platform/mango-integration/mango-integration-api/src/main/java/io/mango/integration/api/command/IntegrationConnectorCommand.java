package io.mango.integration.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 连接器定义保存命令。
 *
 * <p>定义直接覆盖保存，保存即生效。</p>
 */
@Data
@Schema(description = "连接器定义保存命令")
public class IntegrationConnectorCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "连接器编码不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9-]{1,63}$", message = "连接器编码只能包含小写字母、数字和中划线")
    @Schema(description = "连接器编码，例如 wecom-message")
    private String connectorCode;

    @NotBlank(message = "连接器名称不能为空")
    @Size(max = 100, message = "连接器名称最多100个字符")
    @Schema(description = "连接器名称")
    private String connectorName;


    @Size(max = 32, message = "用途分类最多32个字符")
    @Schema(description = "用途分类，取值来自字典 integration_connector_category，例如 PAYMENT、MESSAGE")
    private String category;

    @Size(max = 64, message = "厂商标识最多64个字符")
    @Schema(description = "厂商标识，例如 WECOM")
    private String provider;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    @Schema(description = "状态：0-停用，1-启用")
    private Integer status = 1;

    @Size(max = 8192, message = "配置规范最多8192个字符")
    @Schema(description = "配置规范JSON Schema文本")
    private String configSchema;

    @Size(max = 8192, message = "认证规范最多8192个字符")
    @Schema(description = "认证规范JSON文本")
    private String authSchema;

    @Size(max = 2048, message = "能力清单最多2048个字符")
    @Schema(description = "能力清单JSON数组文本")
    private String capabilitiesJson;

    @Size(max = 10, message = "标签最多10个")
    @Schema(description = "标签列表，用于检索与分类")
    private List<@Size(max = 32, message = "单个标签最多32个字符") String> tags;

    @Size(max = 500, message = "文档地址最多500个字符")
    @Schema(description = "文档地址")
    private String docsUrl;
}
