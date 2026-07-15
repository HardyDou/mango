package io.mango.authorization.api.vo;

import io.mango.authorization.api.enums.DataScopeMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 生效数据权限。
 */
@Data
@Schema(description = "当前主体的生效数据权限")
public class EffectiveDataScopeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "资源编码")
    private String resourceCode;
    @Schema(description = "生效的数据范围模式")
    private DataScopeMode scopeMode;
    @Schema(description = "生效的数据范围值集合")
    private List<String> scopeValues = new ArrayList<>();
    @Schema(description = "是否包含本人数据")
    private Boolean selfIncluded;
    @Schema(description = "是否包含下级范围")
    private Boolean includeChildren;
}
