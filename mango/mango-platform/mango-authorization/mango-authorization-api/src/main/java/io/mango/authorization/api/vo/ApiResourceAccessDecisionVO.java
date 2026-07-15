package io.mango.authorization.api.vo;

import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 运行时 API 资源访问决策。
 *
 * @author hardy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "运行时API资源访问决策")
public class ApiResourceAccessDecisionVO {

    @Schema(description = "是否命中已注册资源")
    private boolean matched;

    @Schema(description = "访问模式")
    private ApiResourceAccessMode accessMode;

    @Schema(description = "访问模式为PERMISSION时需要的权限码")
    private String permissionCode;

    public static ApiResourceAccessDecisionVO unmatched(ApiResourceAccessMode defaultMode) {
        return new ApiResourceAccessDecisionVO(false, defaultMode, null);
    }

    public boolean matched() {
        return matched;
    }

    public ApiResourceAccessMode accessMode() {
        return accessMode;
    }

    public String permissionCode() {
        return permissionCode;
    }
}
