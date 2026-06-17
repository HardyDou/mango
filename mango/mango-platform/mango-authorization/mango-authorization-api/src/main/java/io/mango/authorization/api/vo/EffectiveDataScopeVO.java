package io.mango.authorization.api.vo;

import io.mango.authorization.api.enums.DataScopeMode;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 生效数据权限。
 */
@Data
public class EffectiveDataScopeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String resourceCode;
    private DataScopeMode scopeMode;
    private List<String> scopeValues = new ArrayList<>();
    private Boolean selfIncluded;
    private Boolean includeChildren;
}
