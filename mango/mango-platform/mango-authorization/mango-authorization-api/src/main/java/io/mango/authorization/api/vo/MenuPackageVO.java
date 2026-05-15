package io.mango.authorization.api.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单授权套餐。
 */
@Data
public class MenuPackageVO {

    private Long packageId;
    private String packageName;
    private String packageCode;
    private String appCode;
    private Integer status;
    private Integer sort;
    private String remark;
    private List<Long> menuIds;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
