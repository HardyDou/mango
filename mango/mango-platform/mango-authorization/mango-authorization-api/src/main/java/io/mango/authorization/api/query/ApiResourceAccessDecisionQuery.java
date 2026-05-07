package io.mango.authorization.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * API 资源访问决策查询条件。
 *
 * @author hardy
 */
@Data
@Schema(description = "API 资源访问决策查询条件")
public class ApiResourceAccessDecisionQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "HTTP 方法，例如 GET、POST")
    @NotBlank(message = "HTTP方法不能为空")
    private String httpMethod;

    @Schema(description = "请求路径")
    @NotBlank(message = "路径不能为空")
    private String path;
}
