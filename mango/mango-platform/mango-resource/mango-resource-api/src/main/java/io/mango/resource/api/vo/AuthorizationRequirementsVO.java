package io.mango.resource.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Authorization materialization requirements derived from current Resource declarations.
 */
@Schema(description = "授权物化要求")
@EqualsAndHashCode
public final class AuthorizationRequirementsVO {

    @Schema(description = "菜单物化要求")
    private final List<MenuRequirementVO> menus;

    @Schema(description = "接口物化要求")
    private final List<ApiRequirementVO> apis;

    @Schema(description = "资源声明是否已应用")
    private final boolean sourcesApplied;

    public AuthorizationRequirementsVO(
            List<MenuRequirementVO> menus,
            List<ApiRequirementVO> apis,
            boolean sourcesApplied) {
        this.menus = menus == null ? List.of() : List.copyOf(menus);
        this.apis = apis == null ? List.of() : List.copyOf(apis);
        this.sourcesApplied = sourcesApplied;
    }

    public static AuthorizationRequirementsVO empty() {
        return new AuthorizationRequirementsVO(List.of(), List.of(), false);
    }

    public List<MenuRequirementVO> menus() {
        return menus;
    }

    public List<ApiRequirementVO> apis() {
        return apis;
    }

    public boolean sourcesApplied() {
        return sourcesApplied;
    }
}
