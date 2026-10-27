package io.mango.authorization.api.vo;

import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.enums.ApiResourceSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 运行时 API 资源访问决策。
 *
 * @author hardy
 */
@Data
@NoArgsConstructor
@Schema(description = "运行时API资源访问决策")
public class ApiResourceAccessDecisionVO {

    @Schema(description = "是否命中已注册资源")
    private boolean matched;

    @Schema(description = "访问模式")
    private ApiResourceAccessMode accessMode;

    @Schema(description = "访问模式为PERMISSION时需要的权限码")
    private String permissionCode;

    @Schema(description = "生效资源来源")
    private ApiResourceSource source;

    public ApiResourceAccessDecisionVO(boolean matched,
                                       ApiResourceAccessMode accessMode,
                                       String permissionCode) {
        this(matched, accessMode, permissionCode, null);
    }

    public ApiResourceAccessDecisionVO(boolean matched,
                                       ApiResourceAccessMode accessMode,
                                       String permissionCode,
                                       ApiResourceSource source) {
        this.matched = matched;
        this.accessMode = accessMode;
        this.permissionCode = permissionCode;
        this.source = source;
    }

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

    public ApiResourceSource source() {
        return source;
    }
}
