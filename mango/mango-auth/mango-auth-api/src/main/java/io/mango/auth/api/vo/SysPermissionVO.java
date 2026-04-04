package io.mango.auth.api.vo;

import io.mango.common.vo.BaseVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * System permission VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysPermissionVO extends BaseVO {
    private Long id;
    private String permCode;
    private String permName;
    private String permType;
    private String module;
    private Integer status;
}
