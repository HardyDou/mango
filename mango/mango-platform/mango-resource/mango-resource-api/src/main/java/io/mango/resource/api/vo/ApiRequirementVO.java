package io.mango.resource.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;

/**
 * One expected Authorization API resource row derived from Resource declarations.
 */
@Schema(description = "接口物化要求")
@EqualsAndHashCode
public final class ApiRequirementVO {

    @Schema(description = "模块名称")
    private final String moduleName;

    @Schema(description = "HTTP 方法")
    private final String httpMethod;

    @Schema(description = "路径模式")
    private final String pathPattern;

    @Schema(description = "资源编码")
    private final String resourceCode;

    @Schema(description = "权限编码")
    private final String permissionCode;

    @Schema(description = "访问模式")
    private final String accessMode;

    @Schema(description = "启用状态")
    private final int status;

    public ApiRequirementVO(
            String moduleName,
            String httpMethod,
            String pathPattern,
            String resourceCode,
            String permissionCode,
            String accessMode,
            int status) {
        this.moduleName = moduleName;
        this.httpMethod = httpMethod;
        this.pathPattern = pathPattern;
        this.resourceCode = resourceCode;
        this.permissionCode = permissionCode;
        this.accessMode = accessMode;
        this.status = status;
    }

    public String moduleName() {
        return moduleName;
    }

    public String httpMethod() {
        return httpMethod;
    }

    public String pathPattern() {
        return pathPattern;
    }

    public String resourceCode() {
        return resourceCode;
    }

    public String permissionCode() {
        return permissionCode;
    }

    public String accessMode() {
        return accessMode;
    }

    public int status() {
        return status;
    }
}
