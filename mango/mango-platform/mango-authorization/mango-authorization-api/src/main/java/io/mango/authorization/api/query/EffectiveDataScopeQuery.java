package io.mango.authorization.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 生效数据权限查询。
 */
@Data
@Schema(description = "生效数据权限查询")
public class EffectiveDataScopeQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "资源编码")
    @NotBlank(message = "资源编码不能为空")
    @Size(max = 128, message = "资源编码最多128个字符")
    private String resourceCode;

    @Schema(description = "应用编码；为空时使用当前登录上下文应用编码")
    @Size(max = 64, message = "应用编码最多64个字符")
    private String appCode;
}
