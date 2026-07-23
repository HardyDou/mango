package io.mango.resource.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * One expected Authorization menu row derived from Resource declarations.
 */
@Schema(description = "菜单物化要求")
@EqualsAndHashCode
public final class MenuRequirementVO {

    @Schema(description = "应用编码")
    private final String appCode;

    @Schema(description = "模块编码")
    private final String moduleCode;

    @Schema(description = "菜单编码")
    private final String menuCode;

    @Schema(description = "前端组件路径")
    private final String component;

    @Schema(description = "菜单关联的接口权限编码")
    private final List<String> apiCodes;

    @Schema(description = "启用状态")
    private final int status;

    public MenuRequirementVO(
            String appCode,
            String moduleCode,
            String menuCode,
            String component,
            List<String> apiCodes,
            int status) {
        this.appCode = appCode;
        this.moduleCode = moduleCode;
        this.menuCode = menuCode;
        this.component = component;
        this.apiCodes = apiCodes == null ? List.of() : List.copyOf(apiCodes);
        this.status = status;
    }

    public String appCode() {
        return appCode;
    }

    public String moduleCode() {
        return moduleCode;
    }

    public String menuCode() {
        return menuCode;
    }

    public String component() {
        return component;
    }

    public List<String> apiCodes() {
        return apiCodes;
    }

    public int status() {
        return status;
    }
}
