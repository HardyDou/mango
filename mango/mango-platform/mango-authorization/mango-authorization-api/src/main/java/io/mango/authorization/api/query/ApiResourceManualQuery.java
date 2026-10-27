package io.mango.authorization.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 释放人工接管 API 资源查询。
 */
@Data
@Schema(description = "释放人工接管API资源查询")
public class ApiResourceManualQuery {

    @NotBlank(message = "模块名不能为空")
    @Size(max = 100, message = "模块名不能超过100个字符")
    @Schema(description = "稳定 Mango 模块名，例如 mango-guarantee")
    private String moduleName;

    @NotBlank(message = "HTTP 方法不能为空")
    @Size(max = 16, message = "HTTP 方法不能超过16个字符")
    @Schema(description = "HTTP 方法，例如 GET、POST、ALL")
    private String httpMethod;

    @NotBlank(message = "路径模式不能为空")
    @Size(max = 500, message = "路径模式不能超过500个字符")
    @Schema(description = "Spring MVC 路径模式，例如 /guarantee/open/apply")
    private String pathPattern;
}
