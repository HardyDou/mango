package io.mango.authorization.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 套餐-菜单关联。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("authorization_menu_package_item")
public class MenuPackageItemEntity extends AuthorizationBaseEntity {

    private Long packageId;
    private Long menuId;
    private Integer sort;
}
