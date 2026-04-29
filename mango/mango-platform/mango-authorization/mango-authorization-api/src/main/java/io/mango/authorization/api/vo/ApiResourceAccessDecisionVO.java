package io.mango.authorization.api.vo;

import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 运行时 API 资源访问决策。
 *
 * @param matched 是否命中已注册资源
 * @param accessMode 访问模式
 * @param permissionCode 访问模式为 PERMISSION 时需要的权限码
 *
 * @author hardy
 */
@Schema(description = "运行时 API 资源访问决策")
public record ApiResourceAccessDecisionVO(
        @Schema(description = "是否命中已注册资源")
        boolean matched,
        @Schema(description = "访问模式")
        ApiResourceAccessMode accessMode,
        @Schema(description = "访问模式为 PERMISSION 时需要的权限码")
        String permissionCode) {

    public static ApiResourceAccessDecisionVO unmatched(ApiResourceAccessMode defaultMode) {
        return new ApiResourceAccessDecisionVO(false, defaultMode, null);
    }
}
