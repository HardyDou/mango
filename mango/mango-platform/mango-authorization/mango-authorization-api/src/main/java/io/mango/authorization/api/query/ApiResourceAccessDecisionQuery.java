package io.mango.authorization.api.query;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * API 资源访问决策查询条件。
 *
 * @author hardy
 */
@Data
public class ApiResourceAccessDecisionQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "HTTP方法不能为空")
    private String httpMethod;

    @NotBlank(message = "路径不能为空")
    private String path;
}
