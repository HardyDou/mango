package io.mango.authorization.api.command;

import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 人工接管 API 资源命令。
 *
 * <p>人工接管记录是运行时最高控制来源：已存在的人工接管记录不会被 Controller 扫描、固定声明或 Gateway
 * 默认策略覆盖。释放接管后，系统声明可以重新生效。</p>
 */
@Data
@Schema(description = "人工接管API资源命令")
public class ApiResourceManualCommand {

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

    @NotNull(message = "访问模式不能为空")
    @Schema(description = "人工指定的访问模式：PUBLIC/LOGIN/PERMISSION/INTERNAL")
    private ApiResourceAccessMode accessMode;

    @Size(max = 200, message = "权限码不能超过200个字符")
    @Schema(description = "访问模式为 PERMISSION 时必填的权限码")
    private String permissionCode;

    @Size(max = 500, message = "描述不能超过500个字符")
    @Schema(description = "资源描述，用于管理端识别接管原因")
    private String description;
}
